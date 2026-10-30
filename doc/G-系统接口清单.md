# G-系统接口清单

> 后端：Spring Boot，REST + JSON，基路径 `/api`。
> 鉴权：除注册/登录外，请求头携带 `Authorization: Bearer <token>`（登录/注册返回）。
> 统一响应：`{code:0, msg:"ok", data:{...}}`；业务异常 code>0。
> admin 会话 token 对应虚拟用户 id `admin`，role=`ADMIN`；管理员口令由部署环境提供。

## §3A 前端接口（web 消费子集）

| 方法 | 路径 | 调用方 | 关联需求 | 状态 |
|------|------|--------|----------|------|
| POST | /api/auth/register | 游客 | AUTH-01/02/06/07 | 已实现 |
| POST | /api/auth/login | 游客 | AUTH-01/05 | 已实现 |
| GET | /api/user/me | 已登录 | PROF | 已实现 |
| PUT | /api/user/profile | 客户 | PROF-01/02 | 已实现 |
| PUT | /api/user/avatar | 客户 | PROF-03 | 已实现 |
| GET | /api/user/home | 客户 | LVL-04 / NET-04 | 已实现 |
| POST | /api/user/expand | 客户 | LVL-02/03/06/07 | 已实现 |
| GET | /api/customers | 管理员/客户 | NET-01/04/06 | 已实现 |
| GET | /api/customers/tree/roots | 管理员 | NET-02 | 已实现 |
| GET | /api/customers/{id}/children | 管理员 | NET-02 | 已实现 |
| GET | /api/customers/{id} | 管理员/客户 | NET-05 | 已实现 |
| GET | /api/admin/stats | 管理员 | ADM-03 | 已实现 |
| PUT | /api/admin/customers/{id}/status | 管理员 | ADM-02 | 已实现 |
| DELETE | /api/admin/customers/{id} | 管理员 | ADM-01 | 已实现 |
| PUT | /api/admin/customers/{id}/quota | 管理员 | LVL-03 | 已实现 |
| GET | /api/admin/config | 管理员 | LVL-08 | 已实现 |
| PUT | /api/admin/config | 管理员 | LVL-08 | 已实现 |
| GET | /api/friends | 客户 | IM-02 | 已实现 |
| GET | /api/friends/search?id= | 客户 | IM-01 | 已实现 |
| POST | /api/friends | 客户 | IM-01 | 已实现 |
| GET | /api/groups | 客户 | IM-03 | 已实现 |
| POST | /api/groups | 客户 | IM-03 | 已实现 |
| GET | /api/groups/{id} | 客户 | IM-04/05 | 已实现 |
| POST | /api/groups/{id}/members | 群主 | IM-04 | 已实现 |
| DELETE | /api/groups/{id}/members/{userId} | 群主 | IM-04 | 已实现 |
| PUT | /api/groups/{id}/muted | 群主 | IM-05 | 已实现 |
| GET | /api/messages?scopeType=&peerId=&groupId= | 客户 | IM-02/06 | 已实现 |
| POST | /api/messages | 客户 | IM-07 | 已实现 |

## 1. 认证 /api/auth

### POST /register
req: `{username, password, confirmPwd}`
res: `{token, role:"CUSTOMER", user:UserVO, profileDone:false}`
校验：用户名非空且唯一（≠admin）；密码≥6；两次一致。注册成功即登录态，返回 token，前端弹完善资料提示。

### POST /login
req: `{username, password}`
res: `{token, role, user, profileDone}`
- username=admin：口令须等于部署环境注入的管理员口令，role=ADMIN，user 虚拟。
- 否则按用户名查用户：存在性→密码(BCrypt)→status(disabled 拒绝；deleted 视为不存在)。

## 2. 当前用户 /api/user

- `GET /me` → UserVO（含 role）。
- `PUT /profile` `{name,idcard,phone,address}` → 更新；`profileDone = 全部非空`。
- `PUT /avatar` `{avatar:0..7}`。
- `GET /home` → `{kids:[CustomerNode], kidCount, quota, level, levelName, parent:UserVO|null, canUpgrade, needCount, threshold}`。
- `POST /expand` `{targetId}` → `{ok, upgraded?, newLevel?}`；见 E §3.3/3.4。

## 3. 客户网络 /api/customers

- `GET /?level=&q=&view=list`（客户：仅返回自己下游；管理员：全量过滤）
- `GET /tree/roots`（管理员：无 parent 的活跃客户）
- `GET /{id}/children`（管理员：某节点直接子节点，支持逐级深入）
- `GET /{id}`（管理员任意 / 客户仅自己下游）

## 4. 管理员 /api/admin

- `GET /stats` → StatsVO
- `PUT /customers/{id}/status` `{status:"active"|"disabled"}`
- `DELETE /customers/{id}` → 伪删除（status=deleted）
- `PUT /customers/{id}/quota` `{quota}`
- `GET /config` → `{maxLevel, quota:{1,2,3}, upgrade:{2,3}}`
- `PUT /config` `{quota:{...}, upgrade:{...}}`（maxLevel 只读，不可改）

## 5. 好友 /api/friends

- `GET /` → 好友 UserVO 列表
- `GET /search?id=<11位>` → 候选 UserVO（排除自己、已好友）
- `POST /` `{targetId}` → 对称加好友

## 6. 群组 /api/groups

- `GET /` → 我的群列表（含成员数、最后消息）
- `POST /` `{name, memberIds:[]}`
- `GET /{id}` → 群信息+成员
- `POST /{id}/members` `{userIds:[]}`（群主）
- `DELETE /{id}/members/{userId}`（群主）
- `PUT /{id}/muted` `{muted:0|1}`（群主）

## 7. 消息 /api/messages

- `GET /?scopeType=dm&peerId=` 或 `?scopeType=group&groupId=` → MessageVO[]
- `POST /` `{scopeType, peerId?, groupId?, content}` → MessageVO
  - 群消息：`muted && sender!=owner` 拒绝（REQ-IM-05）。
  - 仅纯文本（REQ-IM-07）。

## 8. 错误码

| code | 含义 |
|------|------|
| 0 | 成功 |
| 40001 | 参数错误 |
| 40101 | 未登录/ token 失效 |
| 40301 | 无权限（非管理员/非群主/越权） |
| 40901 | 用户名已存在 |
| 40902 | 用户名不存在 |
| 40903 | 密码错误 |
| 40904 | 账户已禁用 |
| 40905 | 拓展校验失败（自己/无此人/已有上级/无指标/级别不符） |
| 40906 | 好友校验失败 |
| 40907 | 群校验失败（无名/无成员/禁言） |
| 50000 | 服务器错误 |
