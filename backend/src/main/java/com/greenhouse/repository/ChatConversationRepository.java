package com.greenhouse.repository;

import com.greenhouse.entity.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天对话 Repository
 */
@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    /**
     * 按用户ID分页查询对话
     */
    Page<ChatConversation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 按专家ID分页查询对话
     */
    Page<ChatConversation> findByExpertIdOrderByCreatedAtDesc(Long expertId, Pageable pageable);

    /**
     * 按专家ID和状态查询
     */
    List<ChatConversation> findByExpertIdAndStatusOrderByCreatedAtDesc(Long expertId, ChatConversation.ConversationStatus status);

    /**
     * 按用户ID和状态查询
     */
    List<ChatConversation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ChatConversation.ConversationStatus status);

    /** ADMIN 统计某专家的咨询会话数 */
    long countByExpertId(Long expertId);
}
