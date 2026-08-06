package com.greenhouse.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户自助修改密码请求 DTO（R16，全端通用）
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度8-100位")
    private String newPassword;
}