# N-系统测试指导

> dev 产出策略；执行计划 O 与报告 P 由 tst 唯一产出。测试范围聚焦需求正确性。

## 1. 测试分层与工具

| 层 | 工具 | 范围 |
|----|------|------|
| 单元 | JUnit5 + Mockito（svc）/ Vitest（web） | 领域规则纯函数 |
| API 集成 | Spring Boot Test (`@SpringBootTest` + MockMvc / TestRestTemplate + H2/真实 MySQL) | 端到端链路 |
| 前端 | Vitest + React Testing Library（必要时） | 关键交互 |

> svc 集成测试优先用真实 MySQL（docker/本机）；无 MySQL 时回退 H2 兼容模式（注意 MySQL 方言差异，仅作冒烟）。

## 2. 必测用例（对应需求）

### 后端单元（领域规则）
- N-U01 级别硬上限：maxLevel 配置=4 → 实际 3；=0 → 1。（REQ-LVL-01）
- N-U02 拓展 5 校验：自己 / 目标不存在 / 目标已有上级 / 无指标 / 自己已最高级 → 各自正确拒绝。（REQ-LVL-06）
- N-U03 拓展成功：target.parent/level/quota 正确；me.quota-1。（REQ-LVL-02/06）
- N-U04 自动升级：达门槛且 level>1 → level-1、quota 补齐；未达门槛不升。（REQ-LVL-07）
- N-U05 profileDone：四字段任一空 → false；全填 → true。（REQ-PROF-01）
- N-U06 会话 key：dm 双 id 排序拼接稳定；group=groupId。
- N-U07 群禁言：muted && sender!=owner → 发消息被拒。（REQ-IM-05）
- N-U08 可见性：普通客户查网络仅返回自己下游；跨级不可见。（REQ-LVL-04/NET-04）

### 后端 API 集成
- N-A01 注册→登录→token 鉴权；用户名重复 40901。（AUTH）
- N-A02 admin 写死口令登录；错误口令 40903。（AUTH-05）
- N-A03 拓展全链路 + 升级 + 指标递减。
- N-A04 管理员树形逐级 roots→children→后退。
- N-A05 伪删除后列表不可见，DB 仍存在。（ADM-01）
- N-A06 禁用账户登录被拒 40904。（ADM-02）
- N-A07 stats 数值与种子一致。（ADM-03）
- N-A08 加好友→建群→单聊/群聊；群主禁言后他人发送失败。
- N-A09 越权：普通客户调 admin 接口 → 40301。

### 前端
- N-F01 登录/注册校验与提示。
- N-F02 完善资料 nag：未完善每次登录提示，完善后不提示。（PROF-02）
- N-F03 中英文切换全站生效。（NFR-03）
- N-F04 移动端/桌面端布局可用（≥900px 切换）。

## 3. 门禁
- P0 用例全绿方进入下一迭代；P1 允许遗留记入 X。
- 覆盖率不设硬指标，但领域规则（级别/拓展/升级/可见性/禁言）必须 100% 用例覆盖。

## 4. 验收对照 A §4
逐条手动/自动验证，记入 P-系统测试报告。
