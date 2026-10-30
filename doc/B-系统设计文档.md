# B-系统设计文档

> 架构与分层设计。详细数据见 E/F，接口见 G，业务流程见 D。

## 1. 总体架构
前后端分离：
- **svc**：Spring Boot 3 (JDK21) + MyBatis + MySQL8，REST + JSON，端口 8080。
- **web**：Vite + React 18，独立静态站点，端口 5173（dev），通过 `/api` 代理或直连 svc。

## 2. 后端分层
```
controller  (HTTP 入参校验/响应封装)
   ↓
service     (领域规则：级别/拓展/升级/可见性/禁言)
   ↓
mapper      (MyBatis XML，仅 SQL)
   ↓
MySQL8      (cms_* 6 表)
```
- 横切：`common`(Result/Exception)、`config`(Cors/Mybatis/Interceptor)、`auth`(TokenStore/AuthInterceptor/ThreadLocalUser)。

## 3. 鉴权设计
- 登录/注册成功 → 生成 UUID token → 内存 `ConcurrentHashMap<token, Session{userId,role,expireAt}>`（12h）。
- `AuthInterceptor` 校验 `Authorization: Bearer <token>` → 注入 `ThreadLocalUser`。
- admin：虚拟 Session(userId="admin", role=ADMIN)。
- 领域 service 通过 `ThreadLocalUser` 取当前用户与 role 做可见性/越权判定。

## 4. 配置设计（REQ-LVL-01/08）
- `application.yml: cms.system.max-level`（部署期设定）→ 代码 `HARD_MAX=3` clamp。
- `cms_sys_config` 表存 quota.{1,2,3}、upgrade.{2,3}，管理员可在配置页读写；maxLevel 只读。

## 5. 关键并发与一致性
- 拓展下游：service 内先校验后更新，`me.quota-=1` 与 `target` 更新同事务；高并发下指标超卖风险记入 J（本轮用 DB 行级 + 事务足够，单机演示）。

## 6. 与设计原型对应
前端 UI/交互/文案/i18n 严格对应 `cms/CMS 客户管理系统.dc.html`；后端按 G 接口提供数据，前端不再用 localStorage 模拟（设计原型为前端模拟，实现版改为真实接口）。
