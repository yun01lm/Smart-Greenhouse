package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.User;
import com.greenhouse.module.admin.dto.RoleCountResponse;
import com.greenhouse.module.admin.dto.UpdateUserRequest;
import com.greenhouse.module.admin.dto.UserSummaryResponse;
import com.greenhouse.module.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员用户管理 API
 * <p>
 * 仅 ADMIN 角色可访问（SecurityConfig 中已配置 hasRole('ADMIN')）。
 * 路径前缀：/api/v1/admin
 * </p>
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 用户列表
     * GET /api/v1/admin/users
     *
     * @param role     可选角色筛选（ADMIN/OWNER/WORKER/EXPERT）
     * @param province 可选地区筛选（省/市/县/乡镇/村，从大棚地区字段聚合）
     * @param keyword  可选关键词（用户名/姓名/手机号）
     */
    @GetMapping("/users")
    public ApiResponse<List<UserSummaryResponse>> listUsers(
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.listUsers(role, province, city, district, town, village, keyword));
    }

    /**
     * 用户详情
     * GET /api/v1/admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ApiResponse<UserSummaryResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success(adminService.getUser(userId));
    }

    /**
     * 更新用户（角色/状态/基本信息）
     * PUT /api/v1/admin/users/{userId}
     */
    @PutMapping("/users/{userId}")
    public ApiResponse<UserSummaryResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        Long currentUserId = getCurrentUserId();
        return ApiResponse.success("用户更新成功", adminService.updateUser(currentUserId, userId, request));
    }

    /**
     * 删除用户
     * DELETE /api/v1/admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        adminService.deleteUser(currentUserId, userId);
        return ApiResponse.success("用户已删除", null);
    }

    /**
     * 角色列表 + 统计
     * GET /api/v1/admin/roles
     */
    @GetMapping("/roles")
    public ApiResponse<List<RoleCountResponse>> getRoleStats() {
        return ApiResponse.success(adminService.getRoleStats());
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
