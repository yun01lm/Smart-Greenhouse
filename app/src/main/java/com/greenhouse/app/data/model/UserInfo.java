package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 用户信息模型
 */
public class UserInfo {

    private long id;
    private String username;
    private String role;

    @SerializedName("realName")
    private String realName;

    private String phone;

    // 专家特有字段
    @SerializedName("expertSpecialty")
    private String expertSpecialty;

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getRealName() { return realName; }
    public String getPhone() { return phone; }
    public String getExpertSpecialty() { return expertSpecialty; }

    public void setId(long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setRole(String role) { this.role = role; }
    public void setRealName(String realName) { this.realName = realName; }
    public void setPhone(String phone) { this.phone = phone; }
}
