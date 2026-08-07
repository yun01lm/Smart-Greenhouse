package com.greenhouse.module.permission.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.permission.dto.*;
import com.greenhouse.module.permission.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工管理 API（棚主端，R23 支持技术员）
 * <p>
 * 棚主管理自己的员工（普通员工 WORKER / 技术员 TECHNICIAN）及其权限。
 * 路径前缀：/api/v1/owner/employees
 * </p>
 */
@RestController
@RequestMapping("/api/v1/owner/employees")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 添加/邀请员工
     * POST /api/v1/owner/employees
     */
    @PostMapping
    public ApiResponse<PermissionResponse> addEmployee(@Valid @RequestBody AddEmployeeRequest request) {
        Long ownerId = getCurrentUserId();
        PermissionResponse response = permissionService.addEmployee(ownerId, request);
        return ApiResponse.success("员工添加成功", response);
    }

    /**
     * 员工列表
     * GET /api/v1/owner/employees
     */
    @GetMapping
    public ApiResponse<List<EmployeeResponse>> listEmployees() {
        Long ownerId = getCurrentUserId();
        return ApiResponse.success(permissionService.listEmployees(ownerId));
    }

    /**
     * 重置员工密码（R23：棚主初始化自己名下员工的账号密码）
     * PUT /api/v1/owner/employees/{employeeId}/password
     */
    @PutMapping("/{employeeId}/password")
    public ApiResponse<Void> resetEmployeePassword(
            @PathVariable Long employeeId,
            @Valid @RequestBody ResetEmployeePasswordRequest request) {
        Long ownerId = getCurrentUserId();
        permissionService.resetEmployeePassword(ownerId, employeeId, request.getNewPassword());
        return ApiResponse.success("密码重置成功", null);
    }

    /**
     * 查看员工权限
     * GET /api/v1/owner/employees/{employeeId}/permissions
     */
    @GetMapping("/{employeeId}/permissions")
    public ApiResponse<List<PermissionResponse>> getPermissions(@PathVariable Long employeeId) {
        Long ownerId = getCurrentUserId();
        return ApiResponse.success(permissionService.getEmployeePermissions(ownerId, employeeId));
    }

    /**
     * 更新员工权限
     * PUT /api/v1/owner/employees/{employeeId}/permissions
     */
    @PutMapping("/{employeeId}/permissions")
    public ApiResponse<PermissionResponse> updatePermissions(
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdatePermissionRequest request) {
        Long ownerId = getCurrentUserId();
        PermissionResponse response = permissionService.updatePermission(ownerId, employeeId, request);
        return ApiResponse.success("权限更新成功", response);
    }

    /**
     * 更新员工基本信息
     * PUT /api/v1/owner/employees/{employeeId}
     */
    @PutMapping("/{employeeId}")
    public ApiResponse<EmployeeResponse> updateEmployee(
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        Long ownerId = getCurrentUserId();
        EmployeeResponse response = permissionService.updateEmployee(ownerId, employeeId, request);
        return ApiResponse.success("员工信息更新成功", response);
    }

    /**
     * 删除员工
     * DELETE /api/v1/owner/employees/{employeeId}
     */
    @DeleteMapping("/{employeeId}")
    public ApiResponse<Void> removeEmployee(@PathVariable Long employeeId) {
        Long ownerId = getCurrentUserId();
        permissionService.removeEmployee(ownerId, employeeId);
        return ApiResponse.success("员工已移除", null);
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}