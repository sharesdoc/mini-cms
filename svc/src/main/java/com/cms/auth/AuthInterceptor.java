package com.cms.auth;

import com.cms.common.CurrentUser;
import com.cms.common.Principal;
import com.cms.common.Result;
import com.cms.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器：解析 Authorization: Bearer <token>，校验并注入 CurrentUser。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        // 放行 CORS 预检请求（OPTIONS 无 Authorization 头，否则浏览器跨源 PUT/DELETE 预检会被 401 阻断）
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }
        String auth = req.getHeader("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7).trim();
        }
        Principal p = tokenStore.get(token);
        if (p == null) {
            writeJson(resp, Result.fail(ResultCode.UNAUTHORIZED));
            return false;
        }
        CurrentUser.set(p);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        CurrentUser.clear();
    }

    private void writeJson(HttpServletResponse resp, Result<?> result) throws Exception {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
