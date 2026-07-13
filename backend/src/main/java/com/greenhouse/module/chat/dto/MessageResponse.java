package com.greenhouse.module.chat.dto;

import com.greenhouse.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    /** 消息ID */
    private Long id;

    /** 对话ID */
    private Long conversationId;

    /** 发送者ID */
    private Long senderId;

    /** 发送者身份 */
    private String senderType;

    /** 消息类型 */
    private String messageType;

    /** 文字内容 */
    private String content;

    /** 文件路径 */
    private String filePath;

    /** 环境快照数据 */
    private String snapshotData;

    /** 已读状态 */
    private Integer readStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static MessageResponse fromEntity(ChatMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType().name())
                .messageType(message.getMessageType().name())
                .content(message.getContent())
                .filePath(message.getFilePath())
                .snapshotData(message.getSnapshotData())
                .readStatus(message.getReadStatus())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
