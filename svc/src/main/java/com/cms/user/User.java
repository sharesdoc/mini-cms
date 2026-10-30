package com.cms.user;

import java.time.LocalDateTime;

/**
 * 客户/用户实体（对应 cms_user 表）。
 * id 为 11 位随机数字系统 ID；password 为 BCrypt 哈希；admin 不入库。
 */
public class User {
    private String id;            // 11 位系统 ID
    private String username;      // 登录用户名（手机号或其他）
    private String password;      // BCrypt 哈希
    private String name;          // 姓名
    private String idcard;        // 身份证号
    private String phone;         // 电话号码
    private String address;       // 家庭住址
    private Integer avatar;       // 头像色块索引 0-7
    private Integer level;        // 级别 1/2/3
    private String parentId;      // 上级客户 ID
    private Integer quota;        // 剩余可拓展指标
    private String status;        // active/disabled/deleted
    private Integer profileDone;  // 资料是否完善 0/1
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdcard() { return idcard; }
    public void setIdcard(String idcard) { this.idcard = idcard; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getAvatar() { return avatar; }
    public void setAvatar(Integer avatar) { this.avatar = avatar; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public Integer getQuota() { return quota; }
    public void setQuota(Integer quota) { this.quota = quota; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getProfileDone() { return profileDone; }
    public void setProfileDone(Integer profileDone) { this.profileDone = profileDone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
