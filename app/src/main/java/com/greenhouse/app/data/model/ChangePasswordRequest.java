package com.greenhouse.app.data.model;

/**
 * 修改密码请求体（R16，全端通用）
 */
public class ChangePasswordRequest {

    private String oldPassword;
    private String newPassword;

    public ChangePasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getOldPassword() { return oldPassword; }
    public String getNewPassword() { return newPassword; }
}