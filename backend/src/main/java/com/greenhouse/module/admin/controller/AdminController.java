package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.User;
import com.greenhouse.module.admin.dto.RoleCountResponse;
import com.greenhouse.module.admin.dto.UpdateUserRequest;
import com.greenhouse.module.admin.dto.UserSummaryResponse;
import com.greenhouse.module.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
     * @param role 可选角色筛选（ADMIN/OWNER/WORKER/EXPERT）
     */
    @GetMapping("/users")
    public ApiResponse<List<UserSummaryResponse>> listUsers(
            @RequestParam(required = false) User.Role role) {
        return ApiResponse.success(adminService.listUsers(role));
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
        return ApiResponse.success("用户更新成功", adminService.updateUser(userId, request));
    }

    /**
     * 删除用户
     * DELETE /api/v1/admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
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
}
