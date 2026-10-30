# F-数据库表设计

> 数据库：MySQL 8.x | 字符集：utf8mb4 | 引擎：InnoDB
> 原则：**禁用物理外键**（项目宪法 §2.2），依赖关系由应用层控制。
> 命名规范：表名 `cms_` 前缀 + 下划线小写；主键 `id`。

## 0. 库与初始化

```sql
CREATE DATABASE IF NOT EXISTS cms DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cms;
```

> 超级管理员 admin **不入库**，账号/口令在代码常量中写死（REQ-AUTH-05），故无 admin 数据行。
> 普通用户密码采用 BCrypt 哈希存储；admin 口令按需求字面值（shasum 40 位 hex）单独比对。

## 1. cms_user 客户/用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(11) | PK | 11 位随机数字系统 ID（REQ-AUTH-07） |
| username | VARCHAR(64) | NOT NULL UNIQUE | 登录用户名（手机号或其他） |
| password | VARCHAR(128) | NOT NULL | BCrypt 哈希 |
| name | VARCHAR(64) | | 姓名 |
| idcard | VARCHAR(32) | | 身份证号 |
| phone | VARCHAR(32) | | 电话号码 |
| address | VARCHAR(255) | | 家庭住址 |
| avatar | TINYINT | NOT NULL DEFAULT 0 | 头像色块索引 0-7 |
| level | TINYINT | NOT NULL | 级别 1/2/3 |
| parent_id | VARCHAR(11) | | 上级客户 ID（仅一个，REQ-LVL-05） |
| quota | INT | NOT NULL DEFAULT 0 | 剩余可拓展下游指标 |
| status | VARCHAR(16) | NOT NULL DEFAULT 'active' | active/disabled/deleted |
| profile_done | TINYINT | NOT NULL DEFAULT 0 | 资料是否完善 0/1 |
| created_at | DATETIME | NOT NULL | 注册时间 |

```sql
CREATE TABLE cms_user (
  id           VARCHAR(11)  NOT NULL,
  username     VARCHAR(64)  NOT NULL,
  password     VARCHAR(128) NOT NULL,
  name         VARCHAR(64)  NULL,
  idcard       VARCHAR(32)  NULL,
  phone        VARCHAR(32)  NULL,
  address      VARCHAR(255) NULL,
  avatar       TINYINT      NOT NULL DEFAULT 0,
  level        TINYINT      NOT NULL,
  parent_id    VARCHAR(11)  NULL,
  quota        INT          NOT NULL DEFAULT 0,
  status       VARCHAR(16)  NOT NULL DEFAULT 'active',
  profile_done TINYINT      NOT NULL DEFAULT 0,
  created_at   DATETIME     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_parent (parent_id),
  KEY idx_user_level (level),
  KEY idx_user_status (status),
  KEY idx_user_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 2. cms_friendship 好友关系表（双向对称存储）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| user_id | VARCHAR(11) | PK | 用户 ID |
| friend_id | VARCHAR(11) | PK | 好友 ID |
| created_at | DATETIME | NOT NULL | 建立时间 |

> 添加好友时**对称写入两行**（A→B、B→A），便于 `WHERE user_id=?` 直接查好友列表。

```sql
CREATE TABLE cms_friendship (
  user_id    VARCHAR(11) NOT NULL,
  friend_id  VARCHAR(11) NOT NULL,
  created_at DATETIME    NOT NULL,
  PRIMARY KEY (user_id, friend_id),
  KEY idx_friend_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 3. cms_group 群组表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | VARCHAR(32) | PK | 群 ID（g+时间戳） |
| name | VARCHAR(128) | NOT NULL | 群名称 |
| owner_id | VARCHAR(11) | NOT NULL | 群主 ID（REQ-IM-04） |
| muted | TINYINT | NOT NULL DEFAULT 0 | 全员禁言 0/1（REQ-IM-05） |
| created_at | DATETIME | NOT NULL | 创建时间 |

```sql
CREATE TABLE cms_group (
  id         VARCHAR(32)  NOT NULL,
  name       VARCHAR(128) NOT NULL,
  owner_id   VARCHAR(11)  NOT NULL,
  muted      TINYINT      NOT NULL DEFAULT 0,
  created_at DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_group_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 4. cms_group_member 群成员表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| group_id | VARCHAR(32) | PK | 群 ID |
| user_id | VARCHAR(11) | PK | 成员 ID |
| joined_at | DATETIME | NOT NULL | 入群时间 |

```sql
CREATE TABLE cms_group_member (
  group_id  VARCHAR(32) NOT NULL,
  user_id   VARCHAR(11) NOT NULL,
  joined_at DATETIME    NOT NULL,
  PRIMARY KEY (group_id, user_id),
  KEY idx_gm_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 5. cms_message 消息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK AUTO | 消息 ID |
| scope_type | VARCHAR(8) | NOT NULL | dm（单聊）/ group（群聊） |
| scope_key | VARCHAR(64) | NOT NULL | dm：两 ID 排序拼接 `id1_id2`；group：群 ID |
| sender_id | VARCHAR(11) | NOT NULL | 发送者 ID |
| content | TEXT | NOT NULL | 纯文本内容（REQ-IM-07） |
| created_at | DATETIME | NOT NULL | 发送时间 |

```sql
CREATE TABLE cms_message (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  scope_type VARCHAR(8)   NOT NULL,
  scope_key  VARCHAR(64)  NOT NULL,
  sender_id  VARCHAR(11)  NOT NULL,
  content    TEXT         NOT NULL,
  created_at DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_msg_scope (scope_type, scope_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 6. cms_sys_config 系统配置表（管理员可调）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| cfg_key | VARCHAR(64) | PK | 配置键 |
| cfg_value | VARCHAR(64) | NOT NULL | 配置值 |

> 存储 quota.1 / quota.2 / quota.3（各级默认指标）、upgrade.3 / upgrade.2（升级门槛）。
> **maxLevel 不在此表**：来自部署配置 application.yml，代码硬上限 3（REQ-LVL-01）。

```sql
CREATE TABLE cms_sys_config (
  cfg_key   VARCHAR(64) NOT NULL,
  cfg_value VARCHAR(64) NOT NULL,
  PRIMARY KEY (cfg_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO cms_sys_config(cfg_key, cfg_value) VALUES
  ('quota.1','8'),('quota.2','5'),('quota.3','3'),
  ('upgrade.3','3'),('upgrade.2','5');
```

## 7. 关系说明（应用层维护，无物理外键）

- `cms_user.parent_id → cms_user.id`：客户上级，每个客户仅一个上级。
- `cms_friendship.user_id / friend_id → cms_user.id`：好友关系。
- `cms_group.owner_id → cms_user.id`：群主。
- `cms_group_member.(group_id,user_id) → cms_group.id / cms_user.id`：群成员。
- `cms_message.sender_id → cms_user.id`：消息发送者。

## 8. 初始化数据（种子）

种子客户数据由后端 `data.sql` / DataInitializer 注入（用于演示与测试），与设计原型 seed 逻辑对应：3 名一级客户、若干二三级、1 个禁用账户、1 个群、若干消息。
