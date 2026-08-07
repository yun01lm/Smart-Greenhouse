package com.greenhouse.module.permission.dto;

import com.greenhouse.entity.EmployeePermission;
import com.greenhouse.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 员工信息响应 DTO（棚主视角）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private User.Role role;
    private LocalDateTime createdAt;

    /** 从 User 实体构建（不含权限信息） */
    public static EmployeeResponse fromUser(User user) {
        return EmployeeResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
