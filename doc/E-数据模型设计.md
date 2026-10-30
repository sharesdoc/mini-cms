# E-数据模型设计

> 对应 `F-数据库表设计.md`。本文定义后端实体（Entity）、值对象（DTO）与核心领域规则。

## 1. 实体（Entity，与表 1:1）

### User（cms_user）
```java
id, username, password(BCrypt), name, idcard, phone, address,
avatar(0-7), level(1-3), parentId, quota, status(active|disabled|deleted),
profileDone(boolean), createdAt
```
- `id` 为 11 位随机数字字符串，注册时生成，全局唯一（DB 重试）。
- `password`：BCrypt 哈希存储（admin 不入库，代码常量比对）。

### Friendship / Group / GroupMember / Message / SysConfig —— 同 F 表结构。

## 2. 关键值对象（DTO）

| DTO | 用途 |
|-----|------|
| `LoginRequest{username, password}` | 登录 |
| `RegisterRequest{username, password, confirmPwd}` | 注册 |
| `AuthResponse{token, role, user, profileDone}` | 登录/注册返回 |
| `UserVO` | 用户对外视图（脱敏：不含 password） |
| `HomeSummaryVO` | 首页：下游数、剩余指标、升级进度、上级 |
| `CustomerNodeVO` | 网络节点：id/name/level/quota/status/kidCount/createdAt |
| `StatsVO` | 管理员统计：total/todayNew/weekNew/disabled/7日序列/级别分布/最近注册 |
| `ExpandRequest{targetId}` | 拓展下游 |
| `StatusRequest{status}` | 启停账户 |
| `QuotaRequest{quota}` | 调整指标 |
| `MessageVO{id, senderId, senderName, content, createdAt, mine}` | 消息视图 |
| `ThreadRequest{scopeType, peerId?, groupId?}` | 会话定位 |
| `SendRequest{scopeType, peerId?, groupId?, content}` | 发消息 |

## 3. 核心领域规则（领域服务层实现）

### 3.1 级别硬上限（REQ-LVL-01）
```java
int maxLevel = Math.min(3, config.maxLevel);   // 部署配置 + 代码硬上限
// 任何设置 maxLevel 的入口均 clamp 到 [1,3]，>3 一律视为 3
```

### 3.2 注册默认级别（REQ-AUTH-07 / 普通=最低级）
新用户 `level = maxLevel`（三级体系下即 3），`quota = cfg.quota[maxLevel]`，`parentId=null`。

### 3.3 拓展下游（REQ-LVL-06）
前置校验（任一失败抛业务异常）：
1. `targetId != me.id`（不能拓展自己）
2. 目标存在且 `status != deleted`
3. `target.parentId == null`（无上级）
4. `me.quota > 0`（有剩余指标，原子条件扣减）

> **不再校验 `me.level < maxLevel`**：需求 REQ-LVL-07 + 原文第 17 行明确「普通客户(最低级 level=maxLevel)拓展客户后可升级」。若阻止最低级拓展，则新注册用户(默认 level=maxLevel)永远无法升级，核心机制失效。设计原型原有的 errLevel 检查与此冲突，按铁律 0 以需求为准移除；目标级别由下方 `min(maxLevel, me.level+1)` 封顶，最低级拓展产生的下游同处最低级。

通过后：
- `target.parentId = me.id`
- `target.level = min(maxLevel, me.level + 1)`
- `me.quota -= 1`（原子：`UPDATE ... SET quota=quota-1 WHERE quota>0`）

### 3.4 自动升级（REQ-LVL-07）
拓展成功后，计算 `me` 当前下游数 `count`：
```
threshold = cfg.upgrade[me.level]
if (threshold > 0 && count >= threshold && me.level > 1) {
    me.level -= 1;                          // 向上升一级
    me.quota += cfg.quota[me.level];        // 补齐新级别指标
}
```

### 3.5 可见性（REQ-LVL-04 / REQ-NET-04）
- 普通客户网络列表：仅 `parent_id = me.id` 的直接下游。
- 管理员：全量，可按级别筛选；树形逐级展开（取某节点的直接子节点）。
- 客户详情：普通客户仅能看自己下游的详情；管理员可看任意。（出于业务合理性，普通客户可看自己下游详情，不可跨级）

### 3.6 伪删除（REQ-ADM-01）
`status = 'deleted'`；所有"活跃客户"查询均过滤 `status != 'deleted'`。物理数据保留。

### 3.7 会话 key 规则
- 单聊：`dm` + 两 ID 字典序排序拼接 `min_max`。
- 群聊：`group` + groupId。
消息按 `(scope_type, scope_key, created_at)` 索引查询。

### 3.8 群禁言（REQ-IM-05）
发群消息时：若 `group.muted=1` 且 `sender != owner`，拒绝。

## 4. 状态枚举
- `UserStatus`：ACTIVE / DISABLED / DELETED
- `ScopeType`：DM / GROUP
- `Role`：ADMIN / CUSTOMER

## 5. ID 生成
11 位随机数字：首位 1-9，后 10 位 0-9；DB 唯一冲突重试（最多 10 次）。
