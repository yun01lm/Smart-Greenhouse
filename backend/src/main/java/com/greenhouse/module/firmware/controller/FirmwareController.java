package com.greenhouse.module.firmware.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.Firmware;
import com.greenhouse.module.firmware.dto.FirmwareBatchRequest;
import com.greenhouse.module.firmware.dto.FirmwareResponse;
import com.greenhouse.module.firmware.service.FirmwareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 固件管理 API（仅管理员）
 * <p>
 * 路径前缀 /api/v1/admin/firmwares，权限由 SecurityConfig 统一控制（ADMIN）。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/admin/firmwares")
@RequiredArgsConstructor
public class FirmwareController {

    private final FirmwareService firmwareService;

    /**
     * 批量预注册固件
     * POST /api/v1/admin/firmwares/batch
     */
    @PostMapping("/batch")
    public ApiResponse<List<FirmwareResponse>> batchRegister(@Valid @RequestBody FirmwareBatchRequest request) {
        List<FirmwareResponse> list = firmwareService.batchRegister(request);
        return ApiResponse.success("固件预注册成功，共" + list.size() + "个", list);
    }

    /**
     * 固件列表（可按状态筛选，分页，默认每页15条）
     * GET /api/v1/admin/firmwares?status=UNBOUND&page=1&size=15
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) Firmware.Status status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.success(firmwareService.list(status, page, size));
    }

    /**
     * 固件详情
     * GET /api/v1/admin/firmwares/{firmwareId}
     */
    @GetMapping("/{firmwareId}")
    public ApiResponse<FirmwareResponse> get(@PathVariable String firmwareId) {
        return ApiResponse.success(firmwareService.get(firmwareId));
    }

    /**
     * 未绑定固件统计（Web 固件管理页用）
     * GET /api/v1/admin/firmwares/stats/unbound-count
     */
    @GetMapping("/stats/unbound-count")
    public ApiResponse<Map<String, Object>> unboundCount() {
        return ApiResponse.success(Map.of("count", firmwareService.countUnbound()));
    }
}
