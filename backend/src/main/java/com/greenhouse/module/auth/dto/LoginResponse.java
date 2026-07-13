package com.greenhouse.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    /** JWT Token */
    private String token;

    /** Token类型 */
    @Builder.Default
    private String tokenType = "Bearer";

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色 */
    private String role;

    /** 真实姓名 */
    private String realName;
}
