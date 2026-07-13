package com.greenhouse.module.auth.dto;

import com.greenhouse.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户个人信息响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long id;
    private String username;
    private String phone;
    private String realName;
    private String role;
    private Long ownerId;
    private String expertSpecialty;
    private String expertStatus;
    private Boolean status;
    private LocalDateTime createdAt;

    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .realName(user.getRealName())
                .role(user.getRole().name())
                .ownerId(user.getOwnerId())
                .expertSpecialty(user.getExpertSpecialty())
                .expertStatus(user.getExpertStatus() != null ? user.getExpertStatus().name() : null)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
