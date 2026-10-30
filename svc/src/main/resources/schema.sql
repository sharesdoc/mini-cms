-- CMS 数据库表结构（与 F-数据库表设计.md 一致）
-- 禁用物理外键，依赖关系由应用层控制。所有表 IF NOT EXISTS，幂等。
USE cms;

CREATE TABLE IF NOT EXISTS cms_user (
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

CREATE TABLE IF NOT EXISTS cms_friendship (
  user_id    VARCHAR(11) NOT NULL,
  friend_id  VARCHAR(11) NOT NULL,
  created_at DATETIME    NOT NULL,
  PRIMARY KEY (user_id, friend_id),
  KEY idx_friend_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cms_group (
  id         VARCHAR(32)  NOT NULL,
  name       VARCHAR(128) NOT NULL,
  owner_id   VARCHAR(11)  NOT NULL,
  muted      TINYINT      NOT NULL DEFAULT 0,
  created_at DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_group_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cms_group_member (
  group_id  VARCHAR(32) NOT NULL,
  user_id   VARCHAR(11) NOT NULL,
  joined_at DATETIME    NOT NULL,
  PRIMARY KEY (group_id, user_id),
  KEY idx_gm_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cms_message (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  scope_type VARCHAR(8)   NOT NULL,
  scope_key  VARCHAR(64)  NOT NULL,
  sender_id  VARCHAR(11)  NOT NULL,
  content    TEXT         NOT NULL,
  created_at DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_msg_scope (scope_type, scope_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cms_sys_config (
  cfg_key   VARCHAR(64) NOT NULL,
  cfg_value VARCHAR(64) NOT NULL,
  PRIMARY KEY (cfg_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
