# M-系统开发计划

> 消费 A-F + I，产出本计划 + N + X + Q。技术栈：JDK21 / Spring Boot / MyBatis / MySQL8（svc）+ Vite/React（web）。

## 1. 目录结构

```
cms/
├── CMS-需求文档.md          # 原始需求（用户）
├── cms/                     # 设计原型（.dc.html，只读参考）
├── svc/                     # 后端 Spring Boot 服务
│   ├── pom.xml
│   └── src/main/java/com/cms/{config,common,auth,user,customer,admin,im,system}/
│       └── .../entity|dto|mapper|service|controller
└── web/                     # 前端 Vite+React
    └── src/{api,pages,components,i18n,store}/
```

## 2. 后端架构（分层）

- `config`：CorsConfig、MybatisConfig、WebMvcConfig（注册 AuthInterceptor）、BCrypt 配置。
- `common`：统一响应 `Result<T>`、`ResultCode`、`BizException`、`GlobalExceptionHandler`、`ThreadLocalUser`（当前登录上下文）。
- `auth`：AuthInterceptor、TokenStore（内存 ConcurrentHashMap，12h 过期）、AdminController?（合入 admin 包）、UserService.register/login。
- 领域包：user / customer / admin / im(friend,group,message) / system(config)。
- 每个领域：entity + mapper(xml) + service + controller + dto。

## 3. 关键实现要点（与设计/需求严格对齐）

- **级别硬上限**：`application.yml: cms.system.max-level: 3`；`SystemConfigProvider.maxLevel()` 返回 `Math.min(3, Math.max(1, configured))`，代码常量 `HARD_MAX=3`。
- **admin**：用户名/口令常量；不建 DB 行；token 中 role=ADMIN，虚拟 id="admin"。
- **可见性**：customer/admin 接口在 service 层做 role 校验。
- **i18n**：后端仅返回 code + 数据；错误码前端本地化（与设计原型一致）。后端 msg 用英文兜底。
- **种子数据**：DataInitializer 在空库时插入（3 一级 + 二三级 + 1 禁用 + 1 群 + 消息），对齐设计 seed，便于前端联调。

## 4. 前端实现要点

- 路由：`/login` `/home` `/customers` `/customers/:id` `/stats` `/messages` `/messages/:type/:id` `/me` `/config`。
- 底部导航：客户 [首页/客户/消息/我的]；管理员 [报表/客户/配置/我的]。
- 移动优先：max-width 容器 + 媒体查询 ≥900px 切桌面双栏；橙色 `#FF6A00`。
- i18n：直接迁移设计原型 `T` 字典（zh/en）。
- API：`src/api` 统一 fetch，自动注入 token，401 跳登录。

## 5. 迭代节奏（10 次迭代）

| 迭代 | 目标 |
|------|------|
| 1 | 文档基线 + 双端骨架 + DB schema + 种子数据 |
| 2 | 鉴权 + 用户/资料 + 前端登录注册/我的 |
| 3 | 层级拓展/升级 + home + 客户网络(列表/树/详情) |
| 4 | 管理员 stats + 状态/删除/指标/config |
| 5 | 好友 + 群组 |
| 6 | 消息(单聊/群聊/禁言) + 前端消息页 |
| 7 | 前端全页面联调 + i18n + 移动适配 |
| 8 | /rev 审查 + /tst 单元/API 测试 |
| 9 | /fix 修复 + 回测 |
| 10 | 文档同步、G §3A 回填、收尾 |

## 6. 风险与决策

- **Token 内存存储**：重启失效，I 文档标注为改进项（多实例需 Redis）。
- **密码存储**：BCrypt；admin 走常量。满足项目宪法。
- **maxLevel=1/2 场景**：升级门槛/指标配置键按实际级别存在；代码兼容。
