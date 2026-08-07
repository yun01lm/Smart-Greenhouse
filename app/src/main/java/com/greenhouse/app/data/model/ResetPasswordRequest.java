package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 重置员工密码请求（棚主端，R26）
 */
public class ResetPasswordRequest {

    @SerializedName("newPassword")
    private String newPassword;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String newPassword) { this.newPassword = newPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}