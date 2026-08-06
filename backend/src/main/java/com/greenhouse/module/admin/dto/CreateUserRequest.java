package com.greenhouse.module.admin.dto;

import com.greenhouse.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员新增用户请求 DTO（R16）
 * <p>
 * 初始密码统一为 123456（PasswordPolicy.INITIAL_PASSWORD），由后端设置，不从前端传入。
 * </p>
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50位")
    private String username;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 50, message = "真实姓名最长50字")
    private String realName;

    /** 角色：ADMIN/OWNER/WORKER/EXPERT */
    @NotBlank(message = "角色不能为空")
    private String role;

    /** 员工归属棚主ID（仅WORKER角色必填） */
    private Long ownerId;

    public User.Role getRoleEnum() {
        return User.Role.valueOf(role.toUpperCase());
    }
}