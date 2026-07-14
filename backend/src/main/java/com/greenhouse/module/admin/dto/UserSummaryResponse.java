package com.greenhouse.module.admin.dto;

import com.greenhouse.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户摘要响应 DTO（管理端用户列表用，不含密码）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {

    private Long id;
    private String username;
    private String phone;
    private String realName;
    private User.Role role;
    private Boolean status;
    private Long ownerId;
    private String expertSpecialty;
    private User.ExpertStatus expertStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserSummaryResponse fromEntity(User u) {
        return UserSummaryResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .phone(u.getPhone())
                .realName(u.getRealName())
                .role(u.getRole())
                .status(u.getStatus())
                .ownerId(u.getOwnerId())
                .expertSpecialty(u.getExpertSpecialty())
                .expertStatus(u.getExpertStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
