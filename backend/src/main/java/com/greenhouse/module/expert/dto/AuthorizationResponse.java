package com.greenhouse.module.expert.dto;

import com.greenhouse.entity.DataAuthorization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据授权响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationResponse {

    /** 授权ID */
    private Long id;

    /** 专家ID */
    private Long expertId;

    /** 专家姓名 */
    private String expertName;

    /** 用户ID */
    private Long userId;

    /** 大棚ID */
    private Long greenhouseId;

    /** 大棚名称 */
    private String greenhouseName;

    /** 状态 */
    private String status;

    /** 请求理由 */
    private String reason;

    /** 请求时间 */
    private LocalDateTime requestedAt;

    /** 同意时间 */
    private LocalDateTime approvedAt;

    /** 过期时间 */
    private LocalDateTime expiresAt;

    /** 剩余天数 */
    private Long remainingDays;

    /**
     * 从实体转换
     */
    public static AuthorizationResponse fromEntity(DataAuthorization auth,
                                                     String expertName, String greenhouseName) {
        long remaining = 0;
        if (auth.getExpiresAt() != null && auth.getStatus() == DataAuthorization.AuthorizationStatus.APPROVED) {
            remaining = java.time.Duration.between(LocalDateTime.now(), auth.getExpiresAt()).toDays();
            if (remaining < 0) remaining = 0;
        }

        return AuthorizationResponse.builder()
                .id(auth.getId())
                .expertId(auth.getExpertId())
                .expertName(expertName)
                .userId(auth.getUserId())
                .greenhouseId(auth.getGreenhouseId())
                .greenhouseName(greenhouseName)
                .status(auth.getStatus().name())
                .reason(auth.getReason())
                .requestedAt(auth.getRequestedAt())
                .approvedAt(auth.getApprovedAt())
                .expiresAt(auth.getExpiresAt())
                .remainingDays(remaining)
                .build();
    }
}
