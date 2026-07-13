package com.greenhouse.module.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.*;
import com.greenhouse.module.chat.dto.ConversationRequest;
import com.greenhouse.module.chat.dto.ConversationResponse;
import com.greenhouse.module.chat.dto.MessageResponse;
import com.greenhouse.module.chat.dto.SendMessageRequest;
import com.greenhouse.module.sensor.service.SensorDataService;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时聊天服务
 * <p>
 * 管理对话创建、消息收发、环境快照生成和未读统计。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SensorDataService sensorDataService;
    private final ObjectMapper objectMapper;

    /**
     * 创建对话（发起求助）
     */
    @Transactional
    public ConversationResponse createConversation(Long userId, ConversationRequest request) {
        // 校验专家存在且角色为 EXPERT
        User expert = userRepository.findById(request.getExpertId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERT_NOT_FOUND));
        if (expert.getRole() != User.Role.EXPERT) {
            throw new BusinessException(ErrorCode.EXPERT_NOT_FOUND);
        }

        // 创建对话
        ChatConversation conversation = ChatConversation.builder()
                .userId(userId)
                .expertId(request.getExpertId())
                .greenhouseId(request.getGreenhouseId())
                .subject(request.getSubject())
                .diagnosticId(request.getDiagnosticId())
                .status(ChatConversation.ConversationStatus.WAITING)
                .build();

        conversation = conversationRepository.save(conversation);

        // 发送首条消息（求助描述）
        ChatMessage firstMessage = ChatMessage.builder()
                .conversationId(conversation.getId())
                .senderId(userId)
                .senderType(ChatMessage.SenderType.USER)
                .messageType(ChatMessage.MessageType.TEXT)
                .content(request.getSubject())
                .build();
        messageRepository.save(firstMessage);

        log.info("对话已创建: id={}, user={}, expert={}, subject={}",
                conversation.getId(), userId, request.getExpertId(), request.getSubject());

        return ConversationResponse.fromEntity(conversation,
                expert.getUsername(), "", 0, request.getSubject());
    }

    /**
     * 获取对话列表
     */
    public List<ConversationResponse> getConversations(Long userId, String role, int page, int size) {
        Page<ChatConversation> conversations;

        if ("EXPERT".equalsIgnoreCase(role)) {
            conversations = conversationRepository.findByExpertIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        } else {
            conversations = conversationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        }

        return conversations.getContent().stream()
                .map(conv -> {
                    User otherUser;
                    if ("EXPERT".equalsIgnoreCase(role)) {
                        otherUser = userRepository.findById(conv.getUserId()).orElse(null);
                    } else {
                        otherUser = userRepository.findById(conv.getExpertId()).orElse(null);
                    }

                    long unread = "EXPERT".equalsIgnoreCase(role)
                            ? messageRepository.countUnreadByConversationId(conv.getId())
                            : messageRepository.countUnreadByConversationId(conv.getId());

                    String expertName = otherUser != null ? otherUser.getUsername() : "未知";
                    String lastMsg = getLastMessagePreview(conv.getId());

                    return ConversationResponse.fromEntity(conv, expertName, "", unread, lastMsg);
                })
                .toList();
    }

    /**
     * 获取消息历史
     */
    public List<MessageResponse> getMessages(Long conversationId, Long userId, int page, int size) {
        // 校验参与者身份
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.getUserId().equals(userId) && !conversation.getExpertId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Page<ChatMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId, PageRequest.of(page, size));

        return messages.getContent().stream()
                .map(MessageResponse::fromEntity)
                .toList();
    }

    /**
     * 发送消息（REST 备用通道）
     */
    @Transactional
    public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        // 校验对话存在且未关闭
        ChatConversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (conversation.getStatus() == ChatConversation.ConversationStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CONVERSATION_CLOSED);
        }

        // 判断发送者身份
        ChatMessage.SenderType senderType;
        if (conversation.getUserId().equals(senderId)) {
            senderType = ChatMessage.SenderType.USER;
        } else if (conversation.getExpertId().equals(senderId)) {
            senderType = ChatMessage.SenderType.EXPERT;
        } else {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 专家首次回复时，对话从 WAITING 变为 ACTIVE
        if (senderType == ChatMessage.SenderType.EXPERT
                && conversation.getStatus() == ChatConversation.ConversationStatus.WAITING) {
            conversation.setStatus(ChatConversation.ConversationStatus.ACTIVE);
            conversationRepository.save(conversation);
        }

        // 创建消息
        ChatMessage.MessageType messageType = ChatMessage.MessageType.valueOf(
                request.getMessageType().toUpperCase());

        ChatMessage message = ChatMessage.builder()
                .conversationId(request.getConversationId())
                .senderId(senderId)
                .senderType(senderType)
                .messageType(messageType)
                .content(request.getContent())
                .filePath(request.getFilePath())
                .snapshotData(request.getSnapshotData())
                .build();

        message = messageRepository.save(message);
        log.info("消息已发送: convId={}, sender={}, type={}",
                message.getConversationId(), senderType, messageType);

        return MessageResponse.fromEntity(message);
    }

    /**
     * 发送环境快照
     */
    @Transactional
    public MessageResponse sendSnapshot(Long userId, Long greenhouseId) {
        try {
            // 获取大棚实时传感器数据
            var realtimeData = sensorDataService.getRealtimeData(greenhouseId);

            // 构建快照 JSON
            String snapshotJson = objectMapper.writeValueAsString(realtimeData);

            // 创建快照消息（需要先找到或创建对话，这里简化：直接保存为快照消息）
            ChatMessage message = ChatMessage.builder()
                    .conversationId(0L)  // 由前端指定对话ID后通过 sendMessage 接口发送
                    .senderId(userId)
                    .senderType(ChatMessage.SenderType.USER)
                    .messageType(ChatMessage.MessageType.ENV_SNAPSHOT)
                    .snapshotData(snapshotJson)
                    .build();

            message = messageRepository.save(message);
            return MessageResponse.fromEntity(message);
        } catch (Exception e) {
            log.error("生成环境快照失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 关闭对话
     */
    @Transactional
    public void closeConversation(Long conversationId, Long userId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.getUserId().equals(userId) && !conversation.getExpertId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (conversation.getStatus() == ChatConversation.ConversationStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CONVERSATION_CLOSED);
        }

        conversation.setStatus(ChatConversation.ConversationStatus.CLOSED);
        conversation.setClosedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        log.info("对话已关闭: id={}", conversationId);
    }

    /**
     * 获取未读消息数
     */
    public long getUnreadCount(Long userId, String role) {
        if ("EXPERT".equalsIgnoreCase(role)) {
            return messageRepository.countUnreadByExpertId(userId);
        } else {
            return messageRepository.countUnreadByUserId(userId);
        }
    }

    // ===== 私有方法 =====

    /**
     * 获取最后一条消息的预览文本
     */
    private String getLastMessagePreview(Long conversationId) {
        Page<ChatMessage> lastPage = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId, PageRequest.of(0, 1));
        if (lastPage.hasContent()) {
            ChatMessage lastMsg = lastPage.getContent().get(0);
            if (lastMsg.getMessageType() == ChatMessage.MessageType.TEXT && lastMsg.getContent() != null) {
                String content = lastMsg.getContent();
                return content.length() > 50 ? content.substring(0, 50) + "..." : content;
            }
            return "[" + lastMsg.getMessageType().name() + "]";
        }
        return "";
    }
}
