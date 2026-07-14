package com.greenhouse.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色统计响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleCountResponse {

    /** 角色名 */
    private String role;

    /** 角色中文名 */
    private String roleLabel;

    /** 该角色用户数 */
    private long count;
}
