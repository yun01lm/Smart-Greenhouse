package com.greenhouse.module.chat.dto;

import com.greenhouse.entity.ChatConversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    /** 对话ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 专家ID */
    private Long expertId;

    /** 专家姓名 */
    private String expertName;

    /** 专家专业领域 */
    private String expertSpecialty;

    /** 大棚ID */
    private Long greenhouseId;

    /** 咨询主题 */
    private String subject;

    /** 状态 */
    private String status;

    /** 未读消息数 */
    private long unreadCount;

    /** 最后一条消息内容 */
    private String lastMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static ConversationResponse fromEntity(ChatConversation conversation,
                                                    String expertName, String expertSpecialty,
                                                    long unreadCount, String lastMessage) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .userId(conversation.getUserId())
                .expertId(conversation.getExpertId())
                .expertName(expertName)
                .expertSpecialty(expertSpecialty)
                .greenhouseId(conversation.getGreenhouseId())
                .subject(conversation.getSubject())
                .status(conversation.getStatus().name())
                .unreadCount(unreadCount)
                .lastMessage(lastMessage)
                .createdAt(conversation.getCreatedAt())
                .build();
    }
}
