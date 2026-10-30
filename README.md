# CMS 客户管理系统

一个面向网页端和移动端的客户拓展与客户关系管理示例系统。系统支持客户分级、下游客户拓展、客户资料维护、管理员统计、好友、群组和站内消息。

> 当前仓库只包含匿名演示数据和源码配置示例，不包含真实个人资料、生产凭据或可复用的管理员口令。发布前仍应检查 Git 提交历史。

## 功能

- 用户注册、登录和内存会话管理
- 最多三级客户层级，普通客户可逐级拓展下游客户
- 客户指标、自动升级和管理员临时调整指标
- 管理员客户列表、树状浏览、禁用、伪删除和统计报表
- 客户资料、头像、好友、群组和纯文本消息
- 手机优先的响应式页面和中英文切换
- MySQL 初始化脚本、匿名演示数据和自动化测试

## 技术栈

- 后端：Java 21、Spring Boot 3、MyBatis、MySQL 8
- 前端：React 18、Vite 5、React Router
- 构建：Maven、npm
- 数据库：MySQL 8，应用层维护关联关系，不使用物理外键

## 目录

```text
svc/                         Spring Boot 后端
web/                         React + Vite 前端
dbs/cms-database.sql         数据库结构和匿名配置种子脚本
dbs/init-cms-database.sh    Docker/MySQL 初始化脚本
doc/                         需求、设计、接口、测试和部署文档
install                      本地编译、启动和测试入口
build-configuration          本地启动参数示例，不包含凭据
```

## 环境要求

- JDK 21
- Maven 3.9+
- Node.js 20+
- npm
- Docker Desktop（使用数据库容器时）
- MySQL 8 客户端 `mysql`（执行初始化脚本时）

## 配置凭据

不要把真实密码写入源码、脚本、README、日志或数据库导出文件。启动前在当前 shell 设置环境变量：

```bash
export CMS_DB_PASSWORD='请替换为本地数据库口令'
export CMS_ADMIN_PASSWORD='请替换为本地管理员口令'
export CMS_ADMIN_NAME='admin'
export CMS_CORS_ORIGINS='http://localhost:5173,http://localhost:4173'
```

后端还支持标准 Spring 数据源变量：

```bash
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/cms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true'
export SPRING_DATASOURCE_USERNAME='root'
export SPRING_DATASOURCE_PASSWORD="$CMS_DB_PASSWORD"
```

管理员口令没有仓库内默认值；不设置 `CMS_ADMIN_PASSWORD` 时，管理员登录不可用。

## 启动数据库

创建项目专用 MySQL 容器：

```bash
docker run -d --name cms-database \
  -e MYSQL_ROOT_PASSWORD="$CMS_DB_PASSWORD" \
  -e MYSQL_DATABASE=cms \
  -p 5171:3306 \
  mysql:8.0
```

等待容器就绪后导入数据库：

```bash
DB_PASSWORD="$CMS_DB_PASSWORD" DB_PORT=5171 ./dbs/init-cms-database.sh
```

该脚本只导入表结构和系统配置。首次启动后端时，`DataInitializer` 会生成匿名演示客户；如果设置 `CMS_DEMO_USER_PASSWORD`，可以使用该口令登录演示客户，否则演示客户口令为随机值且不会写入日志。

## 启动应用

分别启动后端和前端：

```bash
cd svc
SPRING_DATASOURCE_URL='jdbc:mysql://localhost:5171/cms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true' \
SPRING_DATASOURCE_USERNAME='root' \
SPRING_DATASOURCE_PASSWORD="$CMS_DB_PASSWORD" \
CMS_ADMIN_PASSWORD="$CMS_ADMIN_PASSWORD" \
mvn spring-boot:run
```

另开终端启动前端：

```bash
cd web
npm install
SVC_PORT=8080 npm run dev
```

默认访问地址：

- 前端：http://localhost:5173
- 后端：http://localhost:8080

如果端口被占用，请通过 Vite 的 `--port` 和 Spring Boot 的 `--server.port` 指定新端口，并把新前端地址加入 `CMS_CORS_ORIGINS`。

## 使用统一脚本

在项目根目录执行：

```bash
export CMS_DB_PASSWORD='本地数据库口令'
export CMS_ADMIN_PASSWORD='本地管理员口令'
./install -b
./install -s
```

停止服务：

```bash
./install -k
```

## 测试和构建

后端测试：

```bash
cd svc
mvn test
```

前端测试和构建：

```bash
cd web
npm test
npm run build
```

## 安全说明

- 生产环境必须使用独立的数据库账号、强管理员口令和正式 HTTPS 域名。
- 不要把 `.env`、日志、JAR、数据库导出文件或真实用户资料提交到 Git。
- 当前 token 存储在内存中，后端重启会使登录会话失效。
- 当前数据库脚本和自动种子数据仅用于本地演示，不应直接用于生产初始化。
- 如果敏感内容曾经进入 Git 历史，仅删除当前文件并不能清除历史记录；发布前应检查历史并按需重写或新建干净仓库。

## 文档

- `doc/A-系统需求文档.md`：系统需求
- `doc/B-系统设计文档.md`：系统设计
- `doc/F-数据库表设计.md`：数据库表设计
- `doc/G-系统接口清单.md`：接口清单
- `doc/H-编译部署说明.md`：编译部署说明
- `doc/N-系统测试指导.md`：测试指导

## 许可证

本项目使用 `PolyForm Noncommercial License 1.0.0`。

- 允许个人研究、学习、实验、测试和非商业兴趣项目使用。
- 允许非营利组织、教育机构和公共研究机构在许可证范围内使用。
- 禁止商业用途；商业授权请联系项目所有者单独协商。
- 本项目不是 OSI 定义的标准开源软件，不得将其宣传为允许商业使用的开源项目。
- 完整条款见仓库根目录的 `LICENSE` 文件。
