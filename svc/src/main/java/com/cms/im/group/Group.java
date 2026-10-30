package com.cms.im.group;

import java.time.LocalDateTime;

/** 群组实体（cms_group） */
public class Group {
    private String id;
    private String name;
    private String ownerId;
    private Integer muted;   // 0/1
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public Integer getMuted() { return muted; }
    public void setMuted(Integer muted) { this.muted = muted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
