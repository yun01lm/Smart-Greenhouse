package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 登录响应
 */
public class LoginResponse {

    private String token;

    @SerializedName("userId")
    private long userId;

    private String username;
    private String role;

    @SerializedName("realName")
    private String realName;

    public String getToken() { return token; }
    public long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getRealName() { return realName; }
}
