package com.greenhouse.module.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置员工密码请求 DTO（R23）
 * <p>
 * 棚主初始化/重置自己名下员工的密码，密码复杂度由 PasswordPolicy 校验。
 * </p>
 */
@Data
public class ResetEmployeePasswordRequest {

    /** 新密码（>=8位，含字母和数字） */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度8-100位")
    private String newPassword;
}