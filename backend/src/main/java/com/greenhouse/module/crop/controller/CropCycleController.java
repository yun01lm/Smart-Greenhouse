package com.greenhouse.module.crop.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.CropCycle;
import com.greenhouse.module.crop.dto.CropCycleRequest;
import com.greenhouse.module.crop.dto.CropCycleResponse;
import com.greenhouse.module.crop.dto.CropTimelineResponse;
import com.greenhouse.module.crop.service.CropCycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 作物生长周期 API
 * <p>
 * 路径前缀：/api/v1/crop-cycles
 * </p>
 *
 * <h3>端点列表</h3>
 * <ul>
 *   <li>GET /api/v1/crop-cycles — 生长周期列表</li>
 *   <li>POST /api/v1/crop-cycles — 创建种植记录</li>
 *   <li>GET /api/v1/crop-cycles/{id} — 周期详情</li>
 *   <li>PUT /api/v1/crop-cycles/{id} — 更新周期</li>
 *   <li>PATCH /api/v1/crop-cycles/{id}/stage — 手动设置阶段</li>
 *   <li>PATCH /api/v1/crop-cycles/{id}/complete — 标记完成</li>
 *   <li>GET /api/v1/crop-cycles/{id}/timeline — 生长时间线</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crop-cycles")
@RequiredArgsConstructor
public class CropCycleController {

    private final CropCycleService cropCycleService;

    /**
     * 生长周期列表（按大棚过滤）
     * GET /api/v1/crop-cycles?greenhouseId=1&status=ACTIVE&page=0&size=10
     */
    @GetMapping
    public ApiResponse<List<CropCycleResponse>> list(
            @RequestParam Long greenhouseId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        CropCycle.CycleStatus cycleStatus = null;
        if (status != null && !status.isBlank()) {
            cycleStatus = CropCycle.CycleStatus.valueOf(status.toUpperCase());
        }

        return ApiResponse.success(cropCycleService.list(greenhouseId, cycleStatus, page, size));
    }

    /**
     * 创建种植记录
     * POST /api/v1/crop-cycles
     */
    @PostMapping
    public ApiResponse<CropCycleResponse> create(@Valid @RequestBody CropCycleRequest request) {
        return ApiResponse.success(cropCycleService.create(request));
    }

    /**
     * 周期详情
     * GET /api/v1/crop-cycles/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<CropCycleResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(cropCycleService.getById(id));
    }

    /**
     * 更新周期
     * PUT /api/v1/crop-cycles/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<CropCycleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CropCycleRequest request) {
        return ApiResponse.success(cropCycleService.update(id, request));
    }

    /**
     * 手动设置生长阶段
     * PATCH /api/v1/crop-cycles/{id}/stage
     */
    @PatchMapping("/{id}/stage")
    public ApiResponse<CropCycleResponse> setStage(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String stage = body.get("stage");
        return ApiResponse.success(cropCycleService.setStage(id, stage));
    }

    /**
     * 标记完成（收获）
     * PATCH /api/v1/crop-cycles/{id}/complete
     */
    @PatchMapping("/{id}/complete")
    public ApiResponse<CropCycleResponse> complete(@PathVariable Long id) {
        return ApiResponse.success(cropCycleService.complete(id));
    }

    /**
     * 生长时间线
     * GET /api/v1/crop-cycles/{id}/timeline
     */
    @GetMapping("/{id}/timeline")
    public ApiResponse<CropTimelineResponse> timeline(@PathVariable Long id) {
        return ApiResponse.success(cropCycleService.getTimeline(id));
    }
}
