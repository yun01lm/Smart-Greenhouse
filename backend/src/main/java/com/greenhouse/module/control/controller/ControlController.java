package com.greenhouse.module.control.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.common.PageResult;
import com.greenhouse.entity.User;
import com.greenhouse.module.control.dto.ControlLogResponse;
import com.greenhouse.module.control.dto.ControlRequest;
import com.greenhouse.module.control.service.ControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备控制 API
 * <p>
 * 路径前缀：/api/v1/control
 * </p>
 */
@RestController
@RequestMapping("/api/v1/control")
@RequiredArgsConstructor
public class ControlController {

    private final ControlService controlService;

    /**
     * 控制单个设备
     * POST /api/v1/control/actuator
     */
    @PostMapping("/actuator")
    public ApiResponse<ControlLogResponse> control(@Valid @RequestBody ControlRequest request) {
        Long userId = getCurrentUserId();
        User.Role role = getCurrentUserRole();
        ControlLogResponse result = controlService.controlDevice(userId, role, request);
        return ApiResponse.success(
                "设备已" + ("ON".equals(request.getAction()) ? "开启" : "关闭"), result);
    }

    /**
     * 查询设备控制日志
     * <p>
     * 1) GET /api/v1/control/logs?deviceId=1  → 按设备查询（不分页，兼容旧调用）
     * 2) GET /api/v1/control/logs?greenhouseId=1&page=0&size=20&source=SCENE → 按大棚分页查询
     * </p>
     */
    @GetMapping("/logs")
    public ApiResponse<?> logs(@RequestParam(required = false) Long deviceId,
                               @RequestParam(required = false) Long greenhouseId,
                               @RequestParam(required = false) String source,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        User.Role role = getCurrentUserRole();

        if (greenhouseId != null) {
            Page<ControlLogResponse> logPage = controlService.getGreenhouseLogs(
                    userId, role, greenhouseId, source, page, size);
            return ApiResponse.success(PageResult.of(logPage));
        }
        if (deviceId != null) {
            return ApiResponse.success(controlService.getDeviceLogs(deviceId));
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "deviceId 或 greenhouseId 至少传一个");
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
