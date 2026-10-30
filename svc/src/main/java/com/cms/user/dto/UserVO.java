package com.cms.user.dto;

import com.cms.common.Role;
import com.cms.user.User;

import java.time.LocalDateTime;

/**
 * 用户对外视图（脱敏：不含 password）。
 */
public record UserVO(
        String id,
        String username,
        String name,
        String idcard,
        String phone,
        String address,
        Integer avatar,
        Integer level,
        String parentId,
        Integer quota,
        String status,
        Boolean profileDone,
        LocalDateTime createdAt,
        Role role
) {
    /** 由实体构建（普通客户角色） */
    public static UserVO of(User u) {
        return new UserVO(
                u.getId(), u.getUsername(), u.getName(), u.getIdcard(), u.getPhone(), u.getAddress(),
                u.getAvatar(), u.getLevel(), u.getParentId(), u.getQuota(), u.getStatus(),
                u.getProfileDone() != null && u.getProfileDone() == 1, u.getCreatedAt(), Role.CUSTOMER);
    }
}
