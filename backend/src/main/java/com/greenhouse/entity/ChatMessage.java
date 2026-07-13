package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 * <p>
 * 对应 DB 第 22 号表 chat_messages。
 * 支持文字(TEXT)、图片(IMAGE)、视频(VIDEO)、环境快照(ENV_SNAPSHOT)四种消息类型。
 * </p>
 */
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_conversation_created", columnList = "conversation_id, created_at"),
        @Index(name = "idx_sender", columnList = "sender_id"),
        @Index(name = "idx_read_status", columnList = "read_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对话ID */
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    /** 发送者ID */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /** 发送者身份 */
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 10)
    private SenderType senderType;

    /** 消息类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 15)
    private MessageType messageType;

    /** 文字内容（TEXT时使用） */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 文件路径（IMAGE/VIDEO时使用） */
    @Column(name = "file_path", length = 255)
    private String filePath;

    /** 环境快照数据（ENV_SNAPSHOT时使用，JSON格式） */
    @Column(name = "snapshot_data", columnDefinition = "JSON")
    private String snapshotData;

    /** 已读状态 */
    @Column(name = "read_status", nullable = false)
    @Builder.Default
    private Integer readStatus = 0;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** 发送者身份 */
    public enum SenderType {
        USER,    // 用户/员工
        EXPERT   // 专家
    }

    /** 消息类型 */
    public enum MessageType {
        TEXT,           // 文字
        IMAGE,          // 图片
        VIDEO,          // 视频
        ENV_SNAPSHOT    // 环境快照
    }
}
