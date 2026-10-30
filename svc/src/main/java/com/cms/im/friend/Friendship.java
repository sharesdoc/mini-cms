package com.cms.im.friend;

import java.time.LocalDateTime;

/** 好友关系实体（cms_friendship，双向对称存储） */
public class Friendship {
    private String userId;
    private String friendId;
    private LocalDateTime createdAt;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFriendId() { return friendId; }
    public void setFriendId(String friendId) { this.friendId = friendId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
