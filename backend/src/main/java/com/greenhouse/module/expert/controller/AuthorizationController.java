package com.greenhouse.module.expert.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.expert.dto.AuthorizationResponse;
import com.greenhouse.module.expert.service.ExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 专家授权 API
 * <p>
 * 路径前缀：/api/v1/expert
 * </p>
 */
@RestController
@RequestMapping("/api/v1/expert")
@RequiredArgsConstructor
public class AuthorizationController {

    private final ExpertService expertService;

    /**
     * 专家发起授权请求
     * POST /api/v1/expert/authorize/request
     */
    @PostMapping("/authorize/request")
    public ApiResponse<AuthorizationResponse> requestAuthorization(@RequestBody Map<String, Object> body) {
        Long expertId = getCurrentUserId();
        Long userId = Long.valueOf(body.get("userId").toString());
        Long greenhouseId = Long.valueOf(body.get("greenhouseId").toString());
        String reason = body.containsKey("reason") ? body.get("reason").toString() : "";
        return ApiResponse.success(expertService.requestAuthorization(expertId, userId, greenhouseId, reason));
    }

    /**
     * 用户查看待处理请求
     * GET /api/v1/expert/authorize/pending
     */
    @GetMapping("/authorize/pending")
    public ApiResponse<List<AuthorizationResponse>> pendingAuthorizations() {
        Long userId = getCurrentUserId();
        return ApiResponse.success(expertService.getPendingAuthorizations(userId));
    }

    /**
     * 用户同意授权
     * PUT /api/v1/expert/authorize/{id}/approve
     */
    @PutMapping("/authorize/{id}/approve")
    public ApiResponse<AuthorizationResponse> approveAuthorization(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(expertService.approveAuthorization(id, userId));
    }

    /**
     * 用户拒绝授权
     * PUT /api/v1/expert/authorize/{id}/reject
     */
    @PutMapping("/authorize/{id}/reject")
    public ApiResponse<Void> rejectAuthorization(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        expertService.rejectAuthorization(id, userId);
        return ApiResponse.success();
    }

    /**
     * 用户撤销授权
     * PUT /api/v1/expert/authorize/{id}/revoke
     */
    @PutMapping("/authorize/{id}/revoke")
    public ApiResponse<Void> revokeAuthorization(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        expertService.revokeAuthorization(id, userId);
        return ApiResponse.success();
    }

    /**
     * 查看有效授权
     * GET /api/v1/expert/authorize/active
     */
    @GetMapping("/authorize/active")
    public ApiResponse<List<AuthorizationResponse>> activeAuthorizations() {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        return ApiResponse.success(expertService.getActiveAuthorizations(userId, role));
    }

    /**
     * 授权历史
     * GET /api/v1/expert/authorize/history
     */
    @GetMapping("/authorize/history")
    public ApiResponse<List<AuthorizationResponse>> history() {
        Long userId = getCurrentUserId();
        String role = getCurrentUserRole();
        return ApiResponse.success(expertService.getActiveAuthorizations(userId, role));
    }

    /**
     * 更新在线状态
     * PUT /api/v1/expert/status
     */
    @PutMapping("/status")
    public ApiResponse<Void> updateStatus(@RequestBody Map<String, Boolean> body) {
        Long expertId = getCurrentUserId();
        boolean isOnline = body.getOrDefault("online", false);
        expertService.updateOnlineStatus(expertId, isOnline);
        return ApiResponse.success();
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
                .map(Object::toString)
                .orElse("OWNER");
    }
}
