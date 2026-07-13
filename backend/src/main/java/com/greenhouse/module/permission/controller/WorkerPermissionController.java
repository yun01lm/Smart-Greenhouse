package com.greenhouse.module.permission.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.permission.dto.PermissionResponse;
import com.greenhouse.module.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 员工权限 API（员工端）
 * <p>
 * 员工查看自己的权限和被授权的大棚。
 * 路径前缀：/api/v1/worker
 * </p>
 */
@RestController
@RequestMapping("/api/v1/worker")
@RequiredArgsConstructor
public class WorkerPermissionController {

    private final PermissionService permissionService;

    /**
     * 查看自己的权限列表
     * GET /api/v1/worker/permissions
     */
    @GetMapping("/permissions")
    public ApiResponse<List<PermissionResponse>> myPermissions() {
        Long workerId = getCurrentUserId();
        return ApiResponse.success(permissionService.getMyPermissions(workerId));
    }

    /**
     * 查看自己可访问的大棚ID列表
     * GET /api/v1/worker/greenhouses
     */
    @GetMapping("/greenhouses")
    public ApiResponse<List<Long>> myGreenhouses() {
        Long workerId = getCurrentUserId();
        return ApiResponse.success(permissionService.getMyGreenhouseIds(workerId));
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
