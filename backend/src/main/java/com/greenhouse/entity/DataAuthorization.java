package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 环境数据授权实体
 * <p>
 * 对应 DB 第 23 号表 data_authorizations。
 * 管理专家查看大棚数据的授权，支持7天有效期和用户随时撤销。
 * </p>
 */
@Entity
@Table(name = "data_authorizations", indexes = {
        @Index(name = "idx_expert_user_greenhouse", columnList = "expert_id, user_id, greenhouse_id"),
        @Index(name = "idx_status_expires", columnList = "status, expires_at"),
        @Index(name = "idx_user_status", columnList = "user_id, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataAuthorization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 专家ID */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /** 用户ID（授权方） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 授权的大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private AuthorizationStatus status = AuthorizationStatus.PENDING;

    /** 请求时间 */
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    /** 同意时间 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 过期时间（同意后+7天） */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** 撤销时间 */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** 撤销操作人 */
    @Column(name = "revoked_by")
    private Long revokedBy;

    /** 请求理由 */
    @Column(name = "reason", length = 255)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }

    /** 授权状态 */
    public enum AuthorizationStatus {
        PENDING,    // 待处理
        APPROVED,   // 已同意
        REJECTED,   // 已拒绝
        EXPIRED,    // 已过期
        REVOKED     // 已撤销
    }
}
