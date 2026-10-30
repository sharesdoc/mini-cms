package com.cms.user.dto;

import jakarta.validation.constraints.NotBlank;

/** 个人资料/头像相关 DTO */
public final class ProfileDtos {

    public record ProfileRequest(
            String name,
            String idcard,
            String phone,
            String address
    ) {}

    public record AvatarRequest(
            @NotBlank(message = "头像不能为空") Integer avatar
    ) {}

    private ProfileDtos() {}
}
