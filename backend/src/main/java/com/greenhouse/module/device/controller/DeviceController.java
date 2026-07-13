package com.greenhouse.module.device.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.Device;
import com.greenhouse.entity.User;
import com.greenhouse.module.device.dto.DeviceRequest;
import com.greenhouse.module.device.dto.DeviceResponse;
import com.greenhouse.module.device.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备管理 API
 * <p>
 * 路径以 /api/v1/greenhouses/{greenhouseId}/devices 为前缀，
 * 表示设备始终属于某个大棚。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/greenhouses/{greenhouseId}/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 获取大棚下设备列表
     * GET /api/v1/greenhouses/{greenhouseId}/devices
     *
     * 可选查询参数：
     * - type: SENSOR / CONTROLLER，按设备类型筛选
     * - status: ONLINE / OFFLINE / ALARM，按状态筛选
     */
    @GetMapping
    public ApiResponse<List<DeviceResponse>> list(
            @PathVariable Long greenhouseId,
            @RequestParam(required = false) Device.DeviceType type,
            @RequestParam(required = false) Device.DeviceStatus status) {

        Long userId = getCurrentUserId();
        User.Role role = getCurrentUserRole();

        List<DeviceResponse> list;
        if (type != null) {
            list = deviceService.listDevicesByType(userId, role, greenhouseId, type);
        } else if (status != null) {
            list = deviceService.listDevicesByStatus(userId, role, greenhouseId, status);
        } else {
            list = deviceService.listDevices(userId, role, greenhouseId);
        }
        return ApiResponse.success(list);
    }

    /**
     * 获取设备详情
     * GET /api/v1/greenhouses/{greenhouseId}/devices/{deviceId}
     */
    @GetMapping("/{deviceId}")
    public ApiResponse<DeviceResponse> get(@PathVariable Long greenhouseId,
                                            @PathVariable Long deviceId) {
        return ApiResponse.success(deviceService.getDevice(deviceId));
    }

    /**
     * 添加设备到指定大棚
     * POST /api/v1/greenhouses/{greenhouseId}/devices
     */
    @PostMapping
    public ApiResponse<DeviceResponse> create(@PathVariable Long greenhouseId,
                                               @Valid @RequestBody DeviceRequest request) {
        Long userId = getCurrentUserId();
        DeviceResponse response = deviceService.createDevice(userId, greenhouseId, request);
        return ApiResponse.success("设备添加成功", response);
    }

    /**
     * 更新设备
     * PUT /api/v1/greenhouses/{greenhouseId}/devices/{deviceId}
     */
    @PutMapping("/{deviceId}")
    public ApiResponse<DeviceResponse> update(@PathVariable Long greenhouseId,
                                               @PathVariable Long deviceId,
                                               @Valid @RequestBody DeviceRequest request) {
        Long userId = getCurrentUserId();
        DeviceResponse response = deviceService.updateDevice(userId, deviceId, request);
        return ApiResponse.success("设备更新成功", response);
    }

    /**
     * 删除设备
     * DELETE /api/v1/greenhouses/{greenhouseId}/devices/{deviceId}
     */
    @DeleteMapping("/{deviceId}")
    public ApiResponse<Void> delete(@PathVariable Long greenhouseId,
                                     @PathVariable Long deviceId) {
        Long userId = getCurrentUserId();
        deviceService.deleteDevice(userId, deviceId);
        return ApiResponse.success("设备删除成功", null);
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    private User.Role getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String roleStr = auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_OWNER");
        return User.Role.valueOf(roleStr.replace("ROLE_", ""));
    }
}
