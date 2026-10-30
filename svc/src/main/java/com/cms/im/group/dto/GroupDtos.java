package com.cms.im.group.dto;

import com.cms.user.dto.PublicUserVO;

import java.time.LocalDateTime;
import java.util.List;

/** 群组相关 DTO */
public final class GroupDtos {

    public record CreateGroupRequest(String name, List<String> memberIds) {}

    public record AddMembersRequest(List<String> userIds) {}

    public record MuteRequest(Integer muted) {}

    /** 群列表项 */
    public record GroupVO(
            String id, String name, String ownerId, Integer muted,
            int memberCount, LocalDateTime createdAt,
            String lastText, LocalDateTime lastTime
    ) {}

    /** 群详情 */
    public record GroupDetail(
            String id, String name, String ownerId, Integer muted,
            boolean isOwner, List<PublicUserVO> members
    ) {}

    private GroupDtos() {}
}
