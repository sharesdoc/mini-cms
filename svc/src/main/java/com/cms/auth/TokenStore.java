package com.cms.auth;

import com.cms.common.Principal;
import com.cms.common.Role;
import com.cms.config.CmsProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 Token 存储：登录/注册成功签发 token，AuthInterceptor 校验。
 * 注：进程内存储，重启失效（架构权衡，见 I-质量控制指导 R1）。
 */
@Component
public class TokenStore {

    /** 会话条目 */
    private static class Session {
        final Principal principal;
        final long expireAt;
        Session(Principal principal, long expireAt) {
            this.principal = principal;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, Session> store = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public TokenStore(CmsProperties props) {
        this.ttlMillis = props.getToken().getExpireHours() * 3600L * 1000L;
    }

    /** 签发 token */
    public String issue(String userId, Role role) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + ttlMillis;
        store.put(token, new Session(new Principal(userId, role), expireAt));
        return token;
    }

    /** 校验 token，过期返回 null */
    public Principal get(String token) {
        if (token == null) return null;
        Session s = store.get(token);
        if (s == null) return null;
        if (System.currentTimeMillis() > s.expireAt) {
            store.remove(token);
            return null;
        }
        return s.principal;
    }

    /** 注销 */
    public void revoke(String token) {
        if (token != null) store.remove(token);
    }
}
