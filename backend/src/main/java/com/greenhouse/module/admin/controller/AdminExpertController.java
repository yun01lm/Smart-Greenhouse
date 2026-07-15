package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.PageResult;
import com.greenhouse.module.admin.service.AdminExpertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员专家工作台 API
 * <p>
 * 路径前缀：/api/v1/admin/experts，仅 ADMIN 角色可访问。
 * 提供全量专家列表、在线状态管理、授权记录查询和统计。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/experts")
@RequiredArgsConstructor
public class AdminExpertController {

    private final AdminExpertService expertService;

    /**
     * 专家列表（含在线状态/咨询数）
     * GET /api/v1/admin/experts
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listExperts() {
        return ApiResponse.success(expertService.listExperts());
    }

    /**
     * 切换专家在线状态
     * PUT /api/v1/admin/experts/{id}/online
     */
    @PutMapping("/{id}/online")
    public ApiResponse<Map<String, Object>> toggleOnline(
            @PathVariable Long id,
            @RequestParam boolean online) {
        log.info("[ADMIN] 切换专家在线状态: expertId={}, online={}", id, online);
        return ApiResponse.success(online ? "已设为在线" : "已设为离线",
                expertService.toggleOnline(id, online));
    }

    /**
     * 全量授权记录（分页）
     * GET /api/v1/admin/experts/authorizations?status=&page=0&size=20
     */
    @GetMapping("/authorizations")
    public ApiResponse<PageResult<Map<String, Object>>> listAuthorizations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Map<String, Object>> result = expertService.listAuthorizations(status, page, size);
        return ApiResponse.success(PageResult.of(
                result.getContent(), result.getTotalElements(), page, size));
    }

    /**
     * 专家工作台统计
     * GET /api/v1/admin/experts/stats
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.success(expertService.getStats());
    }
}
