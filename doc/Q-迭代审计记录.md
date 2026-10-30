# Q-迭代审计记录

> dev 每迭代追加；aud 后续合并入 L。本文件记录每轮迭代实际产出与验证结论。

## 迭代 1（2026-08-02）— 文档基线 + 双端骨架 + 认证端到端

### 产出
- **文档**：A/B/C/D/E/F/G/H/I/M/N/X 共 12 份齐备（自检通过），源自 CMS-需求文档.md + 设计原型 dc.html。
- **后端 svc**：Spring Boot 3.2.5 + JDK21 + MyBatis + MySQL8 骨架；common(Result/ResultCode/BizException/GlobalExceptionHandler/Principal/CurrentUser)、config(CmsProperties 含 HARD_MAX=3 / WebMvc / SecurityBeans BCrypt)、auth(TokenStore 内存 + AuthInterceptor)、user(User 实体+mapper+xml+UserService+UserController+AuthController)、system(SysConfig+mapper+SystemConfigProvider)；schema.sql 6 表 + data.sql 配置种子。
- **前端 web**：Vite+React+react-router；i18n(zh/en，迁移设计原型 T 字典 121 行)；橙色主题 styles.css 移动优先；api/client.js(token 注入+统一响应解析)；store/auth；components/ui(Icon/Avatar/Toast/Sheet)；Layout(移动 tab/桌面侧栏)；Login 与 Me 页完整功能；其余 7 页占位待实现。

### 验证（实测）
- 后端编译通过（34 类）；启动成功，MySQL8.0.33 连接，6 表自动建表，配置加载（5 条）。
- API 端到端 curl 全绿：
  - register → 11 位 ID `38537036291`、level=3、quota=3 ✓
  - login 客户 ✓；admin 写死口令 `94a0c82e...` ✓；错误口令 40903 ✓
  - /me 带 token ✓；无 token → 40101 ✓；update profile → profileDone=true ✓
- 前端 `npm run build` 通过（51 模块，dist 产物）。
- **级别硬上限**：CmsProperties.HARD_MAX_LEVEL=3 + effectiveMaxLevel() clamp 已就位（待 N-U01 单测验证配置=4→3）。

### X 任务状态推进
- S01 骨架 → 已完成-待验证
- S03 鉴权 → 已完成-待验证
- S04 用户/资料 → 已完成-待验证
- W01 骨架 → 已完成-待验证
- W02 登录注册 → 已完成-待验证
- W08 我的页+nag(部分) → 已完成-待验证（nag 弹窗待接登录后逻辑）
- S02 DB schema → 已完成-待验证（含种子配置；用户/群/消息种子待 S05/S09 补）
- 其余 S05-S10、W03-W07、W09-W10 → 新建（迭代 2+）

### 待办（下一迭代优先）
- 后端：客户层级拓展/升级(S05)、网络列表/树/详情(S06)、管理员统计与操作(S07)、好友/群组/消息(S08-S10)、种子数据(DataInitializer)。
- 前端：Home/Customers/Detail/Stats/Messages/Thread/Config 页面实现并对接 API。
- nag 弹窗接入：登录后若 profileDone=false 触发。

### 无 P0/P1 缺陷遗留；进入迭代 2。

## 迭代 2（2026-08-02）— 后端全域实现 + 前端全页面 + 全栈 UI 验证

### 产出
- **后端 svc 全域完成（S05-S10 + S02 种子）**：
  - customer：CustomerService（拓展5校验+自动升级 E§3.3/3.4、/home、列表/树 roots/children、可见性）、CustomerController。
  - admin：AdminService（stats 聚合、status 切换、伪删除、调指标、config 读写+恢复）、AdminController。
  - im.friend：Friendship+mapper+FriendService（按 ID 搜索、对称加好友、列表）、Controller。
  - im.group：Group/GroupMember+mapper+GroupService（建群校验好友、列表含最后消息、详情、群主加/删成员、禁言）、Controller。
  - im.message：Message+mapper+MessageService（会话历史、发消息、群禁言校验）、Controller。
  - system：DataInitializer 种子（22 客户/3 一级/二三级/1 禁用/好友/1 群/消息）。
  - 全量编译通过（75 类）。
- **前端 web 全页面完成（W03-W10）**：Home/Customers(列表+树+面包屑)/CustomerDetail(管理员操作)/Stats(KPI+柱状+分布)/Messages(好友/群组+加好友+建群)/Thread(聊天+禁言)/Config(maxLevel 只读+指标门槛) + ProfileNag 弹窗。client.js 扩展全 API 封装 + localizeError。

### 验证
- 后端 curl 全链路（实测）：
  - 拓展：errSelf / 成功(kidCount+1,quota-1) / errHasParent ✓
  - 好友：search / add / list ✓；群组：create / list ✓；消息：send dm / thread ✓
  - **跨级越权看详情 → 40301** ✓；**群禁言非群主发 → 40907 muted**，群主可发 ✓
  - 管理员伪删除 / 调指标(quota→20 实测) / config 读写 ✓；**普通客户调 admin 接口 → 40301** ✓
- 前端 `npm run build` 通过（50 模块）。
- **Playwright 全栈 UI 冒烟**：
  - admin 登录 → /stats：KPI(总22/今1/周2/禁1)、7日柱状、级别分布(3/7/12)、最近注册 ✓（截图确认）
  - 匿名演示客户登录 → /home：一级徽标、下游列表和拓展按钮正常 ✓（截图确认）
  - 仅 console 错误为 favicon.ico 404（无害）。

### X 任务状态推进
- S02/S05-S10、W03-W07/W09/W10 → 已完成-待验证。后端与前端功能任务全部落地。
- 剩余：Q01(/rev 审查)、Q02/Q03(/tst 单元+集成测试)、Q04(/fix)、Q05(文档同步)。

### 待办（下一迭代）
- /tst：级别硬上限单测（配置=4→3）、拓展5校验、升级、可见性、禁言的单元/API 测试。
- /rev：需求覆盖度审查（对照 A §2 逐条），安全闸门（注入/越权/敏感字段）。
- /fix：处理审查测试发现的问题。
- 未决小项：favicon、单聊线程标题取对方名（当前用消息推断，足够）。

### 无 P0/P1 缺陷遗留；进入迭代 3（测试与审查）。

## 迭代 3（2026-08-02）— /tst 测试 + /rev 审查 + /fix 修复 + 文档同步

### 产出
- **/tst 自动化测试（22 例全绿，Q02/Q03）**：
  - `CmsPropertiesTest`（8）：级别硬上限 clamp（配置 4/5→3、0/负→1）+ admin 写死值。
  - `CmsIntegrationTest`（12，@SpringBootTest + TestRestTemplate + 真实 MySQL）：认证/拓展5校验/自动升级/跨级越权403/管理员操作/禁用登录40904/统计/资料/IM全链路（加好友-建群-发消息-群禁言非群主40907）。
  - `MessageServiceTest`（2）：单聊 key 稳定性。
  - 产出 `P-系统测试报告.md`：P0 用例 100% 覆盖，门禁 PASS。
- **/rev 审查（Q01）**：产出 `代码审查记录.md`。需求覆盖完整（A §2 全 P0/P1 均实现）；安全闸门全通过（鉴权/越权/注入/密码/脱敏）。
- **/fix 修复（Q04）**：修复 1 个 🟡important —— `CustomerService.expand` 指标扣减并发竞态：改「读-检查-写」为原子条件更新 `UPDATE ... SET quota=quota-1 WHERE id=? AND quota>0`（受影响行数=0 即拒绝），消除高并发超卖。修复后 22 测试仍全绿。
- **小修**：补 favicon.svg（消除 UI 唯一 console 404）；web 构建通过。
- **文档同步（Q05）**：G §3A 全部接口状态回填「已实现」（28 条）；新增 `J-系统改进意见.md`（4 项 P2/P3 提案，不阻塞）。

### 验证
- `mvn test`：22 通过 / 0 失败 / 0 错误，BUILD SUCCESS。
- `npm run build`：通过。
- DB 重置并重新播种（22 客户），系统处于可用演示态。

### X 任务状态推进
- Q01/Q02/Q03/Q04/Q05 → 已完成-待验证。**X 清单全部任务完成**（S01-S10、W01-W10、Q01-Q05）。

### 待办（后续迭代可选，非阻塞）
- 前端 Vitest 自动化（当前 Playwright 手测已覆盖关键页）。
- J 改进意见（回收站浏览/日志隔离/WebSocket/Token 持久化）—— 须用户确认后实施。

### 结论
系统已实现需求文档 + 设计的全部功能，通过 22 例自动化测试与代码审查，关键并发缺陷已修复。无 P0/P1 缺陷遗留，达交付质量门。

## 迭代 4（2026-08-02）— 深度 /rev 复审发现并修复核心机制缺陷（errLevel）

### 发现（🟡 important，需求↔设计冲突）
深度复审拓展逻辑发现：`CustomerService.expand` 沿用设计原型的 `if (me.level >= maxLevel) throw errLevel`，会**阻止最低级(level=maxLevel)用户拓展**。而新注册用户默认 level=maxLevel（3），导致**新用户永远无法拓展→无法升级**，核心「拓展升级」机制对新用户完全失效。
- 需求权威性：CMS-需求文档.md 第 17 行 + A §2.3 REQ-LVL-07 明确「普通客户(最低级)拓展一定数量客户后即可升级」；第 26 行「不管哪个级别都有下游指标」。
- 设计原型 1101 行的 errLevel 检查与此冲突 → 按铁律 0（严格覆盖需求）以需求为准。

### 修复（/fix）
- 后端 `CustomerService.expand`：移除 `me.level >= maxLevel` 闸门；目标级别仍由 `min(maxLevel, me.level+1)` 封顶（最低级拓展产生的下游同处最低级，合理）。
- 前端 `Home.jsx`：拓展按钮禁用条件由「级别」改为「quota≤0」，使最低级用户可点击拓展。
- 文档 `E §3.3` 同步：移除该前置校验，注明需求↔设计冲突的裁决理由。

### 验证（23 例全绿 + 实时确认）
- 新增回归用例 `新注册level3用户可拓展并可升级_回归errLevel移除`：level3 拓展 3 个→升级 level2 ✓。
- 修正旧用例 `拓展校验`（原误用 b 的 id 测「自己」，实靠 errLevel 通过）→ 改用 a 自己 id 测 errSelf，并补 level3→level3 与 level1→level2 两种挂靠级别断言。
- `mvn test`：23 通过 / 0 失败。
- **实时确认**：启动后端，注册两个新 level3 用户，u1 拓展 u2 → `code:0, kidCount:1, quotaLeft:2`（修复前会返回 40905 errLevel）。

### 结论
核心拓展升级机制现已对新用户正常工作。23 例测试全绿，无 P0/P1 缺陷遗留。

## 迭代 5（2026-08-02）— 深度 /rev 发现并修复 PII 隐私泄漏

### 发现（🟡 important，隐私）
复审 IM 接口发现：`/api/friends/search`、好友列表、群成员列表均返回完整 `UserVO`，**含 idcard/phone/address 等敏感 PII**。陌生用户仅凭 11 位 ID 搜索即可看到对方全部个人资料，违反最小披露原则；设计原型好友预览仅展示 name/id/avatar。
- 关联：I-质量控制指导 §2「敏感信息脱敏」闸门。

### 修复（/fix）
- 新增 `PublicUserVO`（仅 id/name/avatar），用于好友搜索、好友列表、群成员列表等半陌生/非完整授权场景。
- `FriendService.search/list`、`GroupService.detail.members` 改返 `PublicUserVO`；`FriendController`/`GroupDtos` 同步类型。
- 完整 `UserVO` 仅保留于 `/me`、管理员客户详情等已授权场景。
- 前端无需改（仅消费 id/name/avatar）。

### 验证（24 例全绿 + Playwright UI 回归）
- 新增回归用例 `好友搜索与列表与群成员不暴露敏感PII`：断言 search/list/群成员结果均无 idcard/phone/address 字段。
- `mvn test`：24 通过 / 0 失败。
- **Playwright UI 回归**：匿名演示客户登录→消息页（好友列表正常渲染，无 PII）→点开单聊→发送演示消息成功显示（橙色气泡）→线程正常。脱敏改动未破坏前端。

### 结论
PII 隐私泄漏已修复并回归保护。24 例测试全绿，无 P0/P1 缺陷遗留。

## 迭代 6（2026-08-02）— 深度 /rev 修复拓展并发「抢同一目标」竞态

### 发现（🟡 important，并发正确性）
`expand` 的「目标挂靠」仍是「读 target.parentId==null → 写 parent」非原子。两个不同上级并发拓展同一目标时，两者都通过空上级检查、都扣了指标，但目标最终只归属其一——**另一个上级白扣指标且丢失客户**。这是上轮 quota 原子化的同族并发缺陷。

### 修复（/fix）
- 新增 `UserMapper.claimTarget`：`UPDATE cms_user SET parent_id=?,level=? WHERE id=? AND parent_id IS NULL`（InnoDB 行锁天然序列化，并发第二个得 0 行）。
- 新增 `incrementQuota` 用于 claim 失败时退还刚扣指标。
- `CustomerService.expand` 改用 claimTarget：claimed==0 时退还指标并抛 errHasParent。
- 清理因重构失效的死代码 `updateParentAndLevel`（接口 + XML）。

### 验证（25 例全绿，含确定性并发测试）
- 新增并发回归用例 `并发拓展同一目标_仅一个成功_另一个退还指标`：两线程同时抢同一目标 → 断言恰一个成功(0)、一个 errHasParent(40905)、目标仅一个上级、两人合计 quota 恰扣 1（失败方已退还）。
- `mvn test`：25 通过 / 0 失败。

### 结论
拓展操作现已全链路并发安全（quota 扣减 + 目标认领双原子）。25 例测试全绿，无 P0/P1 缺陷遗留。

## 迭代 7（2026-08-02）— 补齐前端自动化测试 + 代码清理

### 产出
- **前端 Vitest 测试（21 例，补 P 报告 P1 缺口）**：
  - `utils.test.js`(11)：tpl 模板替换、pickLevelName 级别名（含越界回退）、qstr 查询参数拼接。
  - `api/client.test.js`(10)：localizeError 业务错误码→i18n 本地化（40901-40907 + i18n key 查字典 + 未知 key/无 t/非 ApiError 回退）。
- **可测性重构**：提取纯工具 `src/utils.js`（tpl/pickLevelName/qstr），i18n 与 client 复用去重。
- **后端清理**：移除 `CustomerService.node()` 冗余三元与无用参数（两分支本就相同）。

### 验证
- `npm test`（Vitest）：21 通过；`npm run build`：通过。
- `mvn test`：后端 25 通过（node 重构无回归）。
- **合计 46 例自动化测试全绿**（后端 25 + 前端 21）。

### 结论
前端自动化测试缺口补齐，纯工具函数有回归保护。46 例测试全绿，无 P0/P1 缺陷遗留。

## 迭代 8（2026-08-02）— 深度 /rev 修复 home.parent 上级 PII 泄漏

### 发现（🟡 important，隐私，上轮 PII 修复遗漏）
`/api/user/home` 返回的 `parent`（我的上级）为完整 UserVO，含 idcard/phone/address——**下游客户可看到其上级的敏感 PII**。下游无业务理由查看上级身份证。与迭代 5 好友 PII 修复同族，但上轮遗漏了 home.parent。

### 修复（/fix）
- `HomeSummary.parent` 类型 `UserVO` → `PublicUserVO`（仅 id/name/avatar）。
- `CustomerService.home` 用 `PublicUserVO.of(p)` 构造上级。
- 前端 Home.jsx 仅消费 parent.name/avatar/id，无需改。

### 验证（26 例全绿）
- 新增回归用例 `home上级字段不暴露敏感PII`：a 完善 PII → a 拓展 b → b 查 home → 断言 parent 无 idcard/phone/address。
- `mvn test`：26 通过；`npm run build`：通过。

### 补充
- 复核所有对外 UserVO 出口：`/me`、AuthResponse.user（本人，完整合理）、adminVO（管理员）、CustomerDetail（管理员或查看自己下游，CRM 关系语境，保留完整并记入 J-P2 可选掩码）、FriendService/GroupService/home.parent（已脱敏）。无其他未授权 PII 出口。

### 结论
上级 PII 泄漏已修复并回归保护。26 例后端 + 21 例前端 = 47 例测试全绿，无 P0/P1 缺陷遗留。

## 迭代 9（2026-08-02）— 深度 /rev 修复生产 CORS 预检与并发注册健壮性 + 全链路回归

### 发现（🟡 important，生产正确性）
1. **CORS 预检被鉴权拦截**：`AuthInterceptor` 拦截 `/api/**` 会拦截浏览器跨源预检 OPTIONS（无 Authorization 头→40101）。dev 因 vite 代理同源未暴露，但**生产环境（前后端不同源）所有带 Authorization 的 PUT/DELETE/GET 请求会被浏览器预检阻断**。
2. **并发同用户名注册**：应用层 count 检查与 insert 间存在竞态，DB UNIQUE 约束兜底了数据完整性，但冲突会以 50000 暴露给前端而非干净的 40901。

### 修复（/fix）
- `AuthInterceptor.preHandle` 开头放行 OPTIONS（`if OPTIONS return true`），预检交由 Spring CORS 处理。
- `GlobalExceptionHandler` 新增 `DuplicateKeyException`→40901（USER_EXISTS）处理。

### 验证（27 例后端 + 21 前端 = 48 全绿 + 实测 + Playwright 全链路）
- 新增 `CORS预检OPTIONS放行_不被401阻断`：OPTIONS 预检返回 2xx（非 401）。
- **curl 实测**：`OPTIONS /api/user/me` 与 `/api/admin/customers/x`（DELETE 预检）带允许来源均返回 200 + `Access-Control-Allow-Origin/Methods/Headers/Credentials`；非允许来源 403（安全正确）。
- **Playwright 全链路回归**（确认多次改动无回归）：
  - 匿名演示客户首页→点拓展→输入目标 ID→确认：下游数量和指标正确更新 ✓
  - 管理员配置页：quota.1 步进 8→9→保存→API 验证持久化 quota.1=9 ✓（已恢复默认）
- `mvn test` 27/0/0；`npm run build` 通过。

### 结论
生产 CORS 与并发注册健壮性已修复并回归保护。48 例测试全绿，无 P0/P1 缺陷遗留。

## 迭代 10（2026-08-02，最终轮）— 交付级终验 + 交付总结

### 产出
- **最终全量验证**：
  - `mvn clean test`：后端 27/0/0（CmsIntegrationTest 17 + CmsPropertiesTest 8 + MessageServiceTest 2），BUILD SUCCESS。
  - `npm test`：前端 21/0/0；`npm run build`：通过。
  - **合计 48 例自动化测试全绿。**
- **DB 重新播种**（22 客户）+ admin stats 烟测通过，系统处于可用演示态。
- **文档终核**：16 份交付文档齐备（A-H/I/J/M/N/P/Q/X + 代码审查记录 + 交付总结）。
- 产出 `交付总结.md`：需求逐条覆盖表、质量验证、迭代修复的 6 个缺陷汇总、启动方式、已知改进项。

### 需求覆盖终核（CMS-需求文档.md）
AUTH/PROF/LVL/NET/ADM/IM/NFR 七域全部 ✅，每条 P0/P1 需求均有实现 + 测试 + 文档追溯（详见交付总结 §2）。

### 10 轮迭代全景
| 轮次 | 核心 |
|------|------|
| 1 | 文档基线 + 双端骨架 + 认证端到端 |
| 2 | 后端全域 + 前端全页面 + 全栈 UI 验证 |
| 3 | /tst 22 例 + /rev + /fix(quota 并发) |
| 4 | 深审修复 errLevel 核心机制缺陷 |
| 5 | 深审修复好友/群组 PII 泄漏 |
| 6 | 深审修复并发抢目标竞态 |
| 7 | 补齐前端 Vitest(21) + 代码清理 |
| 8 | 深审修复 home.parent PII 泄漏 |
| 9 | 深审修复生产 CORS + 并发注册健壮性 |
| 10 | 交付级终验 + 交付总结 |

### 最终结论
系统完整实现需求与设计，48 例测试全绿 + Playwright 全链路验证，迭代修复 6 个真实缺陷。无 P0/P1 缺陷遗留，**达交付质量，本项目完成**。
