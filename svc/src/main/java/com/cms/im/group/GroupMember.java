package com.cms.im.group;

import java.time.LocalDateTime;

/** 群成员实体（cms_group_member） */
public class GroupMember {
    private String groupId;
    private String userId;
    private LocalDateTime joinedAt;

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
