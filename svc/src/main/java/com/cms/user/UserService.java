package com.cms.user;

import com.cms.auth.TokenStore;
import com.cms.common.BizException;
import com.cms.common.Principal;
import com.cms.common.ResultCode;
import com.cms.common.Role;
import com.cms.config.CmsProperties;
import com.cms.system.SystemConfigProvider;
import com.cms.user.mapper.UserMapper;
import com.cms.user.dto.AuthDtos;
import com.cms.user.dto.ProfileDtos;
import com.cms.user.dto.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 用户/认证领域服务：注册、登录（含 admin 写死账号）、资料维护。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final TokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final CmsProperties props;
    private final SystemConfigProvider cfg;

    public UserService(UserMapper userMapper, TokenStore tokenStore, PasswordEncoder passwordEncoder,
                       CmsProperties props, SystemConfigProvider cfg) {
        this.userMapper = userMapper;
        this.tokenStore = tokenStore;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.cfg = cfg;
    }

    /** 注册：用户名+密码（默认级别=最低级 maxLevel） */
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        String username = req.username().trim();
        String password = req.password();
        if (password == null || password.length() < 6) {
            throw new BizException(ResultCode.BAD_PARAM, "密码至少 6 位");
        }
        if (!password.equals(req.confirmPwd())) {
            throw new BizException(ResultCode.BAD_PARAM, "两次输入的密码不一致");
        }
        // admin 用户名为保留字
        if (username.equalsIgnoreCase(props.getAdmin().getName())) {
            throw new BizException(ResultCode.USER_EXISTS);
        }
        if (userMapper.countByUsername(username) > 0) {
            throw new BizException(ResultCode.USER_EXISTS);
        }
        int maxLevel = cfg.maxLevel();
        User u = new User();
        u.setId(generateId());
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        // 姓名默认取用户名末 4 位（与设计原型一致）
        u.setName(username.length() > 6 ? username.substring(username.length() - 4) : username);
        u.setIdcard("");
        u.setPhone(username.matches("\\d{11}") ? username : "");
        u.setAddress("");
        u.setAvatar(ThreadLocalRandom.current().nextInt(8));
        u.setLevel(maxLevel);
        u.setParentId(null);
        u.setQuota(cfg.quota(maxLevel));
        u.setStatus("active");
        u.setProfileDone(0);
        u.setCreatedAt(LocalDateTime.now());
        userMapper.insert(u);

        String token = tokenStore.issue(u.getId(), Role.CUSTOMER);
        UserVO vo = UserVO.of(u);
        return new AuthDtos.AuthResponse(token, Role.CUSTOMER, vo, false);
    }

    /** 登录：admin 走写死口令；普通用户走 DB + BCrypt */
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        String username = req.username().trim();
        String password = req.password();
        // 超级管理员
        if (username.equalsIgnoreCase(props.getAdmin().getName())) {
            if (!props.getAdmin().getPassword().equals(password)) {
                throw new BizException(ResultCode.PWD_WRONG);
            }
            String token = tokenStore.issue("admin", Role.ADMIN);
            UserVO vo = adminVO();
            return new AuthDtos.AuthResponse(token, Role.ADMIN, vo, true);
        }
        User u = userMapper.selectByUsername(username);
        if (u == null || "deleted".equals(u.getStatus())) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(password, u.getPassword())) {
            throw new BizException(ResultCode.PWD_WRONG);
        }
        if ("disabled".equals(u.getStatus())) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        String token = tokenStore.issue(u.getId(), Role.CUSTOMER);
        UserVO vo = UserVO.of(u);
        boolean done = u.getProfileDone() != null && u.getProfileDone() == 1;
        return new AuthDtos.AuthResponse(token, Role.CUSTOMER, vo, done);
    }

    /** 当前用户视图 */
    public UserVO me(Principal p) {
        if (p.isAdmin()) return adminVO();
        User u = mustGet(p.getUserId());
        return UserVO.of(u);
    }

    /** 更新资料，返回 profileDone */
    public boolean updateProfile(String userId, ProfileDtos.ProfileRequest req) {
        User u = mustGet(userId);
        String name = trimOrEmpty(req.name());
        String idcard = trimOrEmpty(req.idcard());
        String phone = trimOrEmpty(req.phone());
        String address = trimOrEmpty(req.address());
        boolean done = !name.isBlank() && !idcard.isBlank() && !phone.isBlank() && !address.isBlank();
        userMapper.updateProfile(userId,
                name.isBlank() ? u.getName() : name, idcard, phone, address,
                done ? 1 : 0);
        return done;
    }

    public void updateAvatar(String userId, int avatar) {
        int a = Math.min(7, Math.max(0, avatar));
        userMapper.updateAvatar(userId, a);
    }

    /** 退出：注销 token */
    public void logout(String token) {
        tokenStore.revoke(token);
    }

    // ---- helpers ----

    public User mustGet(String id) {
        User u = userMapper.selectById(id);
        if (u == null) throw new BizException(ResultCode.USER_NOT_FOUND);
        return u;
    }

    /** 生成 11 位随机数字 ID（首位 1-9），冲突重试 */
    private String generateId() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(1 + ThreadLocalRandom.current().nextInt(9));
            for (int j = 0; j < 10; j++) sb.append(ThreadLocalRandom.current().nextInt(10));
            String id = sb.toString();
            if (userMapper.selectById(id) == null) return id;
        }
        throw new BizException(ResultCode.SERVER_ERROR, "ID 生成失败，请重试");
    }

    private UserVO adminVO() {
        return new UserVO("00000000000", props.getAdmin().getName(), "超级管理员", "", "", "",
                6, 0, null, 0, "active", true, LocalDateTime.now(), Role.ADMIN);
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
