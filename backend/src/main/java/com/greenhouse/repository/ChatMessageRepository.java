package com.greenhouse.repository;

import com.greenhouse.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天消息 Repository
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 按对话ID分页查询消息（时间正序）
     */
    Page<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    /**
     * 按对话ID分页查询消息（时间倒序，取最新消息预览用）
     */
    Page<ChatMessage> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    /**
     * 按对话ID查询全部消息（时间正序，R9 管理端明细/导出用）
     */
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * 统计对话消息数（R9）
     */
    long countByConversationId(Long conversationId);

    /**
     * 批量统计多个对话的消息数（R9）
     */
    @Query("SELECT m.conversationId, COUNT(m) FROM ChatMessage m WHERE m.conversationId IN :ids GROUP BY m.conversationId")
    List<Object[]> countByConversationIds(@Param("ids") List<Long> ids);

    /**
     * 统计未读消息数
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.senderType = 'EXPERT' AND m.readStatus = 0")
    long countUnreadByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 统计用户所有对话的未读消息数
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m JOIN ChatConversation c ON m.conversationId = c.id WHERE c.userId = :userId AND m.senderType = 'EXPERT' AND m.readStatus = 0")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 统计专家所有对话的未读消息数
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m JOIN ChatConversation c ON m.conversationId = c.id WHERE c.expertId = :expertId AND m.senderType = 'USER' AND m.readStatus = 0")
    long countUnreadByExpertId(@Param("expertId") Long expertId);

    /**
     * 将对话中的消息标记为已读
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.readStatus = 1 WHERE m.conversationId = :conversationId AND m.senderType = :senderType AND m.readStatus = 0")
    void markAsRead(@Param("conversationId") Long conversationId, @Param("senderType") ChatMessage.SenderType senderType);
}
