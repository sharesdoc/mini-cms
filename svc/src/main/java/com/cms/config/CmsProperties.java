package com.cms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * CMS 业务配置（绑定 application.yml 的 cms.* ）。
 * - system.maxLevel：部署期设定的客户级别数；代码层 HARD_MAX=3 强制上限。
 * - admin.name/password：从运行环境注入的超级管理员账号配置。
 * - cors.origins：允许的前端来源。
 * - token.expireHours：token 有效期。
 */
@ConfigurationProperties(prefix = "cms")
public class CmsProperties {

    private System system = new System();
    private Admin admin = new Admin();
    private Cors cors = new Cors();
    private Token token = new Token();

    /** 级别硬上限：任何配置都不可超过 3（REQ-LVL-01） */
    public static final int HARD_MAX_LEVEL = 3;

    public System getSystem() { return system; }
    public void setSystem(System system) { this.system = system; }
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Token getToken() { return token; }
    public void setToken(Token token) { this.token = token; }

    /** 有效最大级别：clamp 到 [1, HARD_MAX_LEVEL] */
    public int effectiveMaxLevel() {
        int cfg = system.getMaxLevel();
        return Math.min(HARD_MAX_LEVEL, Math.max(1, cfg));
    }

    public static class System {
        private int maxLevel = 3;
        public int getMaxLevel() { return maxLevel; }
        public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }
    }

    public static class Admin {
        private String name = "admin";
        /** 管理员口令必须由运行环境注入，源码不提供默认口令。 */
        private String password = "";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Cors {
        private List<String> origins = List.of("http://localhost:5173");
        public List<String> getOrigins() { return origins; }
        public void setOrigins(List<String> origins) { this.origins = origins; }
    }

    public static class Token {
        private int expireHours = 12;
        public int getExpireHours() { return expireHours; }
        public void setExpireHours(int expireHours) { this.expireHours = expireHours; }
    }
}
