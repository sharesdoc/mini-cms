package com.cms.auth;

import com.cms.common.CurrentUser;
import com.cms.common.Result;
import com.cms.user.UserService;
import com.cms.user.dto.AuthDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证接口：注册 / 登录 / 退出 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return Result.ok(userService.register(req));
    }

    @PostMapping("/login")
    public Result<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return Result.ok(userService.login(req));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest req) {
        CurrentUser.require();
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            userService.logout(auth.substring(7).trim());
        }
        return Result.ok();
    }
}
