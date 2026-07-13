package com.greenhouse.module.auth.dto;

import com.greenhouse.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度6-100位")
    private String password;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 50, message = "真实姓名最长50字")
    private String realName;

    /** 角色：OWNER（棚主）或 WORKER（员工），APP端无ADMIN */
    @NotBlank(message = "角色不能为空")
    private String role;

    /** 员工归属棚主ID（仅WORKER角色需要填写） */
    private Long ownerId;

    /** 专家专业领域（仅EXPERT角色需要填写） */
    private String expertSpecialty;

    public User.Role getRoleEnum() {
        return User.Role.valueOf(role.toUpperCase());
    }
}
