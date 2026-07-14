package com.greenhouse.module.admin.dto;

import com.greenhouse.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员更新用户请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 角色 */
    @NotNull(message = "角色不能为空")
    private User.Role role;

    /** 账号状态：true=启用，false=禁用 */
    private Boolean status;
}
