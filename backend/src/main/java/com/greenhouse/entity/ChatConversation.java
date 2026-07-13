package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 专家咨询对话实体
 * <p>
 * 对应 DB 第 21 号表 chat_conversations。
 * 管理用户与专家之间的咨询对话会话。
 * </p>
 */
@Entity
@Table(name = "chat_conversations", indexes = {
        @Index(name = "idx_user_expert", columnList = "user_id, expert_id"),
        @Index(name = "idx_expert_status", columnList = "expert_id, status"),
        @Index(name = "idx_status_created", columnList = "status, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户/员工ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 专家ID */
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    /** 关联大棚ID */
    @Column(name = "greenhouse_id")
    private Long greenhouseId;

    /** 咨询主题 */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /** 状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.WAITING;

    /** 关联诊断记录ID */
    @Column(name = "diagnostic_id")
    private Long diagnosticId;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 关闭时间 */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** 对话状态 */
    public enum ConversationStatus {
        WAITING,  // 等待中（专家尚未响应）
        ACTIVE,   // 进行中
        CLOSED    // 已关闭
    }
}
