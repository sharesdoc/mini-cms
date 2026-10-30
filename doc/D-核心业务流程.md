# D-核心业务流程

## 1. 注册→完善资料（REQ-AUTH / PROF）
1. 输入用户名(默认提示手机号)+密码(≥6)+确认密码。
2. 校验用户名唯一(≠admin)→ 生成 11 位 ID → level=maxLevel, quota=cfg.quota[maxLevel] → BCrypt 存库。
3. 发 token，前端跳首页 + 弹"完善资料"nag。
4. 完善姓名/身份证/电话/住址 → profileDone=true → 不再 nag。

## 2. 拓展下游→自动升级（REQ-LVL）
1. 客户在首页/快捷操作点"拓展客户"，输入对方 11 位 ID。
2. 校验：非自己 / 存在且未删 / 对方无上级 / 自己有指标 / 自己未达最高级。
3. 通过：对方 parentId=我、level=min(max,我.level+1)、我.quota-=1。
4. 复算我下游数 count：若 upgrade[我.level]>0 且 count≥门槛 且 我.level>1 → 我.level-=1，quota+=cfg.quota[新level]，提示升级。
5. 失败 → 对应错误码 toast。

## 3. 管理员浏览客户树（REQ-NET）
1. /customers 树形视图 → roots(无上级活跃客户)。
2. 点节点 → 该节点 children（直接下游）→ 面包屑累积。
3. 点叶子或详情按钮 → /customers/:id 详情。
4. 后退按面包屑逐级返回。列表视图支持级别筛选 + 搜索。

## 4. 管理员统计（REQ-ADM-03）
聚合：total(活跃)、todayNew(今日0点起)、weekNew(近7日)、disabled、近7日每日序列、级别分布占比、最近5注册。

## 5. 添加好友→单聊（REQ-IM-01/02/07）
1. 消息页"添加好友" → 输入 11 位 ID 搜索 → 勾选 → 对称入库。
2. 好友列表点人 → 线程(scope=dm, key=排序拼接) → 拉历史 + 发纯文本。

## 6. 建群→群聊→禁言（REQ-IM-03/04/05/06/07）
1. "创建群组" → 输入群名 + 勾选好友 → 建 group + members(含群主)。
2. 群列表点群 → 线程(scope=group, key=groupId)。
3. 群主可：加/删成员、切换 muted。
4. 发群消息：muted && sender!=owner → 拒绝（40907）。

## 7. 账户管理（REQ-ADM-01/02）
- 禁用：status=disabled → 该用户登录被拒(40904)。
- 激活：status=active。
- 删除：status=deleted → 列表/登录均不可见，DB 保留。
