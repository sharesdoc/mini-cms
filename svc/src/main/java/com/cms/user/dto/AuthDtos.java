package com.cms.user.dto;

import com.cms.common.Role;
import jakarta.validation.constraints.NotBlank;

/**
 * 认证相关 DTO 集中定义。
 */
public final class AuthDtos {

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {}

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password,
            @NotBlank(message = "确认密码不能为空") String confirmPwd
    ) {}

    /** 登录/注册返回 */
    public record AuthResponse(String token, Role role, UserVO user, Boolean profileDone) {}

    private AuthDtos() {}
}
