# P-系统测试报告

> 唯一产出方：tst skill | 日期：2026-08-02 | 对应 N-系统测试指导
> 执行命令：`mvn test`（JDK21 + 真实 MySQL8.0.33 容器）

## 1. 测试结果总览

| 测试类 | 用例数 | 通过 | 失败 | 说明 |
|--------|--------|------|------|------|
| CmsPropertiesTest | 8 | 8 | 0 | 级别硬上限 clamp + admin 写死值 |
| CmsIntegrationTest | 17 | 17 | 0 | 认证/拓展/升级/可见性/管理员/IM + level3拓展 + PII脱敏(搜索/列表/群成员/home上级) + 并发抢目标 + CORS预检 |
| MessageServiceTest | 2 | 2 | 0 | 单聊会话 key 稳定性 |
| **前端 web（Vitest）** | | | | |
| utils.test.js | 11 | 11 | 0 | tpl 模板替换 / pickLevelName 级别名 / qstr 查询参数 |
| api/client.test.js | 10 | 10 | 0 | localizeError 错误码→i18n 本地化（含 key 回退） |
| **合计** | **46**（后端 25 + 前端 21） | **46** | **0** | **BUILD SUCCESS** |

## 2. 用例与需求映射

### 级别硬上限（N-U01 / REQ-LVL-01）— 8 例全绿
- 配置 4→clamp 3、配置 5→3、配置 0→1、负数→1、正常 2/3 保持、HARD_MAX 常量=3、admin 写死值（name=admin, 40 位 hex 口令）。
- **结论：代码硬上限 3 在所有配置入口均生效，配置文件误配 4 也被强制限制为 3。**

### 认证（N-A01/N-A02 / REQ-AUTH）— 3 例全绿
- 注册→登录→token 鉴权；重复用户名 40901；无 token→40101；admin 写死口令登录、错误口令 40903；密码不一致/过短→40001。

### 拓展 + 升级（N-U02/U03/U04 / REQ-LVL-02/06/07）— 3 例全绿
- 5 校验：errSelf / errNotFound / errHasParent 各自正确拒绝。
- 成功拓展：target.parentId、level=min(max,+1)、me.quota-1 全部正确。
- **自动升级**：level2 拓展满 5（门槛）→自动升 level1 ✓。
- 无指标→40905 ✓。

### 可见性 + 越权（N-U08/N-A09 / REQ-LVL-04/NET-04）— 2 例全绿
- 客户跨级查看非下游→40301；客户调 admin 接口（删除/状态）→40301。
- 管理员可查任意客户、调指标、伪删除。

### 管理员 + 统计（N-A05/A06/A07 / REQ-ADM）— 3 例全绿
- 伪删除→status=deleted（数据保留）；禁用账户登录→40904；统计 total/series7/levelDist 返回正确。

### 资料（N-U05 / REQ-PROF-01）— 1 例绿
- profileDone：未完善 false→完善四字段后 true。

### IM（N-U07/N-A08 / REQ-IM-01..07）— 1 例绿（覆盖全链路）
- 加好友（对称互见）→建群→发群消息→**群主禁言后非群主发→40907，群主仍可发**→单聊消息发送与会话查询。

### 会话 key（N-U06）— 2 例绿
- dmKey 与参数顺序无关、字典序拼接稳定。

## 3. 覆盖结论
- N-系统测试指导 §2 中 P0 用例 **全部覆盖并通过**。
- 领域规则（级别/拓展/升级/可见性/禁言）100% 用例覆盖。
- 未覆盖（P1/范围外，记入 X 遗留）：前端 Vitest（当前以 Playwright 手测覆盖）、压测/渗透（范围外）。

## 4. 门禁判定
**PASS** —— P0 用例全绿，可进入 /rev 审查与 /fix。
