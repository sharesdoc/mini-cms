package com.cms.user.dto;

import com.cms.user.User;

/**
 * 公开用户视图（脱敏）：仅暴露 id/name/avatar，不含 idcard/phone/address 等敏感 PII。
 * 用于好友搜索、好友列表、群成员列表等非完整资料授权场景，防止 PII 泄漏。
 */
public record PublicUserVO(
        String id,
        String name,
        Integer avatar
) {
    public static PublicUserVO of(User u) {
        if (u == null) return null;
        return new PublicUserVO(u.getId(), u.getName(), u.getAvatar());
    }
}
