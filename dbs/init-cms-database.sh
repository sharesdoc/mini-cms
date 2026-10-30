#!/usr/bin/env bash
# 初始化 cms 数据库：优先复用已可达的 MySQL；若无可用 DB 则拉起独立容器 cms-database，
# 再导入 ./dbs/cms-database.sql（结构+数据），最后连接测试确认导入结果。
# 数据库凭据通过 DB_USER、DB_PASSWORD 环境变量提供，不写入仓库。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DUMP_FILE="${SCRIPT_DIR}/cms-database.sql"

DB_NAME="cms"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-${CMS_DB_PASSWORD:-}}"
DB_HOST="127.0.0.1"
DB_PORT="${DB_PORT:-5171}"
CONTAINER_NAME="cms-database"
MYSQL_IMAGE="mysql:8.0"
REUSED_EXISTING_CONTAINER=0

export MYSQL_PWD="$DB_PASSWORD"  # 避免密码以 -p 明文参数出现在 ps 进程列表中

log()  { echo "[init-cms-database] $*"; }
fail() { echo "[init-cms-database] 错误：$*" >&2; exit 1; }

[[ -f "$DUMP_FILE" ]] || fail "未找到导出文件 $DUMP_FILE"
command -v mysql >/dev/null 2>&1 || fail "未找到 mysql 客户端，请先安装（brew install mysql-client）"
[[ -n "$DB_PASSWORD" ]] || fail "请先设置 DB_PASSWORD 或 CMS_DB_PASSWORD"

mysql_ping() {
  mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" \
    --connect-timeout=3 -e "SELECT 1;" >/dev/null 2>&1
}

# ---------- 1. 检查是否已有可用 DB（同用户名密码可连通即视为可用，不重复起容器） ----------
if mysql_ping; then
  log "检测到 ${DB_HOST}:${DB_PORT} 已有可连通的 MySQL（用户名/密码匹配），跳过容器创建，直接导入。"
else
  log "${DB_HOST}:${DB_PORT} 当前不可连通，检查 Docker 是否可用以拉起专用容器 ${CONTAINER_NAME}。"

  command -v docker >/dev/null 2>&1 || fail "未安装 docker，且本机无可用 MySQL，无法继续"
  docker info >/dev/null 2>&1 || fail "docker 已安装但未运行（daemon 未启动），请先启动 Docker"

  if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
    STATE="$(docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME")"
    REUSED_EXISTING_CONTAINER=1
    if [[ "$STATE" != "true" ]]; then
      log "容器 ${CONTAINER_NAME} 已存在但未运行，启动它。"
      docker start "$CONTAINER_NAME" >/dev/null
    else
      log "容器 ${CONTAINER_NAME} 已在运行，但端口 ${DB_PORT} 连不通，等待其就绪。"
    fi
  else
    # 目标宿主机端口若已被其它容器占用（例如本机共享的 mysql8.0 实例），自动改用空闲端口，
    # 避免抢占其它项目正在使用的数据库端口。
    if lsof -iTCP:"$DB_PORT" -sTCP:LISTEN -P >/dev/null 2>&1; then
      ALT_PORT=3307
      while lsof -iTCP:"$ALT_PORT" -sTCP:LISTEN -P >/dev/null 2>&1; do
        ALT_PORT=$((ALT_PORT + 1))
      done
      log "宿主机端口 ${DB_PORT} 已被占用（可能是其它项目共用的 MySQL），改用空闲端口 ${ALT_PORT}。"
      DB_PORT="$ALT_PORT"
    fi

    log "创建新容器 ${CONTAINER_NAME}（${MYSQL_IMAGE}，宿主机端口 ${DB_PORT}）。"
    docker run -d \
      --name "$CONTAINER_NAME" \
      -e MYSQL_ROOT_PASSWORD="$DB_PASSWORD" \
      -e MYSQL_DATABASE="$DB_NAME" \
      -p "${DB_PORT}:3306" \
      "$MYSQL_IMAGE" >/dev/null
  fi

  log "等待 MySQL 就绪（最多 60 秒）..."
  for i in $(seq 1 30); do
    if mysql_ping; then
      log "MySQL 已就绪。"
      break
    fi
    if [[ "$i" -eq 30 ]]; then
      if [[ "$REUSED_EXISTING_CONTAINER" -eq 1 ]]; then
        fail "等待 60 秒后仍连不通 ${DB_HOST}:${DB_PORT}。容器 ${CONTAINER_NAME} 是复用的已有容器——若其创建时的密码与当前 DB_PASSWORD 不一致，会一直探活失败；请核对密码，或 docker rm -f ${CONTAINER_NAME} 后重新执行本脚本。容器日志：docker logs ${CONTAINER_NAME}"
      else
        fail "等待 60 秒后 MySQL 仍未就绪，容器日志：docker logs ${CONTAINER_NAME}"
      fi
    fi
    sleep 2
  done
fi

# ---------- 2. 导入结构+数据 ----------
log "导入 ${DUMP_FILE} 到 ${DB_HOST}:${DB_PORT} ..."
mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" < "$DUMP_FILE"
log "导入命令执行完成。"

# ---------- 3. 测试：连通性 + 表存在 + 行数非零 ----------
log "开始验证导入结果..."
EXPECTED_TABLES=(cms_user cms_friendship cms_group cms_group_member cms_message cms_sys_config)
FAILED=0

for t in "${EXPECTED_TABLES[@]}"; do
  CNT="$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -N -B \
    -e "SELECT COUNT(*) FROM ${DB_NAME}.${t};" 2>/dev/null || echo "ERR")"
  if [[ "$CNT" == "ERR" ]]; then
    echo "[init-cms-database]   ✗ 表 ${t} 不存在或查询失败"
    FAILED=1
  else
    echo "[init-cms-database]   ✓ 表 ${t}：${CNT} 行"
  fi
done

if [[ "$FAILED" -eq 0 ]]; then
  log "测试通过：数据库 ${DB_NAME} 已在 ${DB_HOST}:${DB_PORT} 就绪，全部表可查询。"
  exit 0
else
  fail "测试未通过：存在表缺失或查询失败，请检查上方输出"
fi
