package com.cms.common;

/**
 * 线程局部上下文：AuthInterceptor 解析 token 后注入当前主体，请求结束清理。
 */
public final class CurrentUser {
    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    public static void set(Principal p) { HOLDER.set(p); }
    public static Principal get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    /** 必须为管理员，否则抛 FORBIDDEN */
    public static Principal requireAdmin() {
        Principal p = HOLDER.get();
        if (p == null || !p.isAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return p;
    }

    /** 必须为普通客户，返回其 userId */
    public static String requireCustomerId() {
        Principal p = HOLDER.get();
        if (p == null || p.isAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return p.getUserId();
    }

    /** 必须已登录 */
    public static Principal require() {
        Principal p = HOLDER.get();
        if (p == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return p;
    }
}
