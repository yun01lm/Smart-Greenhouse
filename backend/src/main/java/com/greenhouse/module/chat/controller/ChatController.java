package com.greenhouse.module.chat.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.chat.dto.ConversationRequest;
import com.greenhouse.module.chat.dto.ConversationResponse;
import com.greenhouse.module.chat.dto.MessageResponse;
import com.greenhouse.module.chat.dto.SendMessageRequest;
import com.greenhouse.module.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 专家咨询对话 API
 * <p>
 * 路径前缀：/api/v1/chat
 * </p>
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 创建对话（发起求助）
     * POST /api/v1/chat/conversations
     */
    @PostMapping("/conversations")
    public ApiResponse<ConversationResponse> createConversation(@Valid @RequestBody ConversationRequest request) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(chatService.createConversation(userId, request));
    }

    /**
     * 对话列表
     * GET /api/v1/chat/conversations?status=ACTIVE&page=0&size=10
     */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        return ApiResponse.success(chatService.getConversations(userId, role, page, size));
    }

    /**
     * 对话消息历史
     * GET /api/v1/chat/conversations/{id}/messages?page=0&size=20
     */
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();
        return ApiResponse.success(chatService.getMessages(id, userId, page, size));
    }

    /**
     * 发送消息（REST备用）
     * POST /api/v1/chat/messages
     */
    @PostMapping("/messages")
    public ApiResponse<MessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(chatService.sendMessage(userId, request));
    }

    /**
     * 发送环境快照
     * POST /api/v1/chat/snapshot
     */
    @PostMapping("/snapshot")
    public ApiResponse<MessageResponse> sendSnapshot(@RequestBody Map<String, Long> body) {
        Long userId = getCurrentUserId();
        Long greenhouseId = body.get("greenhouseId");
        return ApiResponse.success(chatService.sendSnapshot(userId, greenhouseId));
    }

    /**
     * 关闭对话
     * PUT /api/v1/chat/conversations/{id}/close
     */
    @PutMapping("/conversations/{id}/close")
    public ApiResponse<Void> closeConversation(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        chatService.closeConversation(id, userId);
        return ApiResponse.success();
    }

    /**
     * 重新开启会话
     * PUT /api/v1/chat/conversations/{id}/reopen
     */
    @PutMapping("/conversations/{id}/reopen")
    public ApiResponse<Void> reopenConversation(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        chatService.reopenConversation(id, userId);
        return ApiResponse.success();
    }

        /**
     * 未读消息数
     * GET /api/v1/chat/unread
     */
    @GetMapping("/unread")
    public ApiResponse<Map<String, Long>> unreadCount() {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        long count = chatService.getUnreadCount(userId, role);
        return ApiResponse.success(Map.of("count", count));
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("OWNER");
    }
}
