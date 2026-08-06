package com.greenhouse.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置用户密码请求 DTO（R16）
 * <p>
 * 需传入该用户当前绑定的手机号进行一致性验证，验证通过后才允许修改密码。
 * </p>
 */
@Data
public class AdminResetPasswordRequest {

    /** 绑定手机号（验证用，必须与该用户当前手机号一致） */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度8-100位")
    private String newPassword;
}