package com.cms.user;

import com.cms.common.CurrentUser;
import com.cms.common.Result;
import com.cms.customer.CustomerService;
import com.cms.customer.dto.CustomerDtos;
import com.cms.user.dto.ProfileDtos;
import com.cms.user.dto.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户接口：资料、头像、首页汇总、拓展下游 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final CustomerService customerService;

    public UserController(UserService userService, CustomerService customerService) {
        this.userService = userService;
        this.customerService = customerService;
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.me(CurrentUser.require()));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody ProfileDtos.ProfileRequest req) {
        String uid = CurrentUser.requireCustomerId();
        userService.updateProfile(uid, req);
        return Result.ok();
    }

    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@Valid @RequestBody ProfileDtos.AvatarRequest req) {
        String uid = CurrentUser.requireCustomerId();
        userService.updateAvatar(uid, req.avatar());
        return Result.ok();
    }

    /** 客户首页汇总 */
    @GetMapping("/home")
    public Result<CustomerDtos.HomeSummary> home() {
        String uid = CurrentUser.requireCustomerId();
        return Result.ok(customerService.home(uid));
    }

    /** 拓展下游（REQ-LVL-06） */
    @PostMapping("/expand")
    public Result<CustomerDtos.ExpandResult> expand(@RequestBody CustomerDtos.ExpandRequest req) {
        String uid = CurrentUser.requireCustomerId();
        return Result.ok(customerService.expand(uid, req.targetId()));
    }
}
