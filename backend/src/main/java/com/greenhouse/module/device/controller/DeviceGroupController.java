package com.greenhouse.module.device.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.User;
import com.greenhouse.module.device.dto.DeviceGroupRequest;
import com.greenhouse.module.device.dto.DeviceGroupResponse;
import com.greenhouse.module.device.service.DeviceGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备分组管理 API
 * <p>
 * 路径以 /api/v1/greenhouses/{greenhouseId}/device-groups 为前缀。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/greenhouses/{greenhouseId}/device-groups")
@RequiredArgsConstructor
public class DeviceGroupController {

    private final DeviceGroupService groupService;

    /**
     * 获取大棚下所有分组
     * GET /api/v1/greenhouses/{greenhouseId}/device-groups
     */
    @GetMapping
    public ApiResponse<List<DeviceGroupResponse>> list(@PathVariable Long greenhouseId) {
        return ApiResponse.success(groupService.listGroups(greenhouseId));
    }

    /**
     * 获取分组详情
     * GET /api/v1/greenhouses/{greenhouseId}/device-groups/{groupId}
     */
    @GetMapping("/{groupId}")
    public ApiResponse<DeviceGroupResponse> get(@PathVariable Long greenhouseId,
                                                 @PathVariable Long groupId) {
        return ApiResponse.success(groupService.getGroup(groupId));
    }

    /**
     * 创建分组
     * POST /api/v1/greenhouses/{greenhouseId}/device-groups
     */
    @PostMapping
    public ApiResponse<DeviceGroupResponse> create(@PathVariable Long greenhouseId,
                                                    @Valid @RequestBody DeviceGroupRequest request) {
        Long userId = getCurrentUserId();
        DeviceGroupResponse response = groupService.createGroup(userId, greenhouseId, request);
        return ApiResponse.success("分组创建成功", response);
    }

    /**
     * 更新分组
     * PUT /api/v1/greenhouses/{greenhouseId}/device-groups/{groupId}
     */
    @PutMapping("/{groupId}")
    public ApiResponse<DeviceGroupResponse> update(@PathVariable Long greenhouseId,
                                                    @PathVariable Long groupId,
                                                    @Valid @RequestBody DeviceGroupRequest request) {
        Long userId = getCurrentUserId();
        DeviceGroupResponse response = groupService.updateGroup(userId, groupId, request);
        return ApiResponse.success("分组更新成功", response);
    }

    /**
     * 向分组添加设备
     * POST /api/v1/greenhouses/{greenhouseId}/device-groups/{groupId}/devices/{deviceId}
     */
    @PostMapping("/{groupId}/devices/{deviceId}")
    public ApiResponse<DeviceGroupResponse> addDevice(@PathVariable Long greenhouseId,
                                                       @PathVariable Long groupId,
                                                       @PathVariable Long deviceId) {
        Long userId = getCurrentUserId();
        DeviceGroupResponse response = groupService.addDeviceToGroup(userId, groupId, deviceId);
        return ApiResponse.success("设备已加入分组", response);
    }

    /**
     * 从分组移除设备
     * DELETE /api/v1/greenhouses/{greenhouseId}/device-groups/{groupId}/devices/{deviceId}
     */
    @DeleteMapping("/{groupId}/devices/{deviceId}")
    public ApiResponse<DeviceGroupResponse> removeDevice(@PathVariable Long greenhouseId,
                                                          @PathVariable Long groupId,
                                                          @PathVariable Long deviceId) {
        Long userId = getCurrentUserId();
        DeviceGroupResponse response = groupService.removeDeviceFromGroup(userId, groupId, deviceId);
        return ApiResponse.success("设备已移出分组", response);
    }

    /**
     * 删除分组
     * DELETE /api/v1/greenhouses/{greenhouseId}/device-groups/{groupId}
     */
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> delete(@PathVariable Long greenhouseId,
                                     @PathVariable Long groupId) {
        Long userId = getCurrentUserId();
        groupService.deleteGroup(userId, groupId);
        return ApiResponse.success("分组删除成功", null);
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
