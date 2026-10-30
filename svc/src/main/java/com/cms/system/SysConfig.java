package com.cms.system;

/** 系统配置实体（cms_sys_config）：存储各级默认指标 quota.N 与升级门槛 upgrade.N */
public class SysConfig {
    private String cfgKey;
    private String cfgValue;

    public String getCfgKey() { return cfgKey; }
    public void setCfgKey(String cfgKey) { this.cfgKey = cfgKey; }
    public String getCfgValue() { return cfgValue; }
    public void setCfgValue(String cfgValue) { this.cfgValue = cfgValue; }
}
