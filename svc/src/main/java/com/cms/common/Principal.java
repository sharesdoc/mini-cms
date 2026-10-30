package com.cms.common;

/**
 * 当前登录主体（线程局部）。admin 的 userId 固定为 "admin"，role=ADMIN。
 */
public class Principal {
    private final String userId;
    private final Role role;

    public Principal(String userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public String getUserId() { return userId; }
    public Role getRole() { return role; }

    public boolean isAdmin() { return role == Role.ADMIN; }
}
