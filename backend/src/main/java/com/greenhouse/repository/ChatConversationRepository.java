package com.greenhouse.repository;

import com.greenhouse.entity.ChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 聊天对话 Repository
 */
@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long>,
        JpaSpecificationExecutor<ChatConversation> {

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

    /**
     * 查询某用户与某专家之间未关闭（进行中/等待中）的最新会话（R35 同专家复用）
     */
    Optional<ChatConversation> findTopByUserIdAndExpertIdAndStatusNotOrderByCreatedAtDesc(
            Long userId, Long expertId, ChatConversation.ConversationStatus status);

    /**
     * 查询某用户与某专家之间最近关闭的会话（R35 关闭后复用重新开启）
     */
    Optional<ChatConversation> findTopByUserIdAndExpertIdAndStatusOrderByClosedAtDesc(
            Long userId, Long expertId, ChatConversation.ConversationStatus status);

    /** ADMIN 统计某专家的咨询会话数 */
    long countByExpertId(Long expertId);
}
