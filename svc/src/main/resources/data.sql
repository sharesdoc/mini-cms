-- CMS 系统配置默认值（INSERT IGNORE 幂等）。maxLevel 不在此表（来自 application.yml，代码硬上限 3）。
USE cms;
INSERT IGNORE INTO cms_sys_config(cfg_key, cfg_value) VALUES
  ('quota.1','8'),('quota.2','5'),('quota.3','3'),
  ('upgrade.3','3'),('upgrade.2','5');
