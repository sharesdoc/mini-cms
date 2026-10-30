# H-编译部署说明

## 1. 环境要求
- JDK 21、Maven 3.9+、Node 20+、MySQL 8.x（实测 MySQL 9.x 客户端兼容）。
- conda base（项目宪法 §4，仅运行脚本/工具时）。

## 2. 数据库准备
```bash
mysql -uroot -p < svc/src/main/resources/schema.sql   # 建库建表 + 种子数据
```
或启动 svc 时由 DataInitializer 自动建表（首次空库）。

## 3. 后端 svc
```bash
cd svc
# 配置（通过环境变量提供数据源和管理员凭据；cms.system.max-level 可按需调整）
mvn -DskipTests clean package
SPRING_DATASOURCE_PASSWORD="$CMS_DB_PASSWORD" \
CMS_ADMIN_PASSWORD="$CMS_ADMIN_PASSWORD" \
java -jar target/cms-svc-1.0.0.jar
# 默认 http://localhost:8080
```
关键环境变量：`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`CMS_ADMIN_NAME`、`CMS_ADMIN_PASSWORD`、`CMS_CORS_ORIGINS`。管理员口令不再写入源码或文档。

## 4. 前端 web
```bash
cd web
npm install
npm run dev      # http://localhost:5173（vite 代理 /api → 8080）
npm run build    # 产物 dist/，可由 nginx 部署
```

## 5. 部署注意
- maxLevel 修改须改 application.yml 并重启；**不可超过 3**（代码 HARD_MAX 强制）。
- Token 为内存存储，svc 重启会使所有会话失效，需重新登录。
- 生产：前端 dist 由静态服务器托管，`/api` 反代到 svc；CORS origins 改为生产域名。
