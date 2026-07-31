package com.greenhouse.module.growth.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.PageResult;
import com.greenhouse.module.growth.dto.GrowthImageResponse;
import com.greenhouse.module.growth.dto.GrowthResponse;
import com.greenhouse.module.growth.service.GrowthService;
import com.greenhouse.security.annotations.RequireGreenhouseAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 长势评估 API
 * <p>路径前缀：/api/v1/growth</p>
 */
@RestController
@RequestMapping("/api/v1/growth")
@RequiredArgsConstructor
public class GrowthController {

    private final GrowthService growthService;

    /**
     * 最新长势评估
     * GET /api/v1/growth/latest?greenhouseId=1
     */
    @GetMapping("/latest")
    @RequireGreenhouseAccess
    public ApiResponse<GrowthResponse> latest(@RequestParam Long greenhouseId) {
        return ApiResponse.success(growthService.getLatest(greenhouseId));
    }

    /**
     * 长势历史（分页）
     * GET /api/v1/growth/history?greenhouseId=1&page=1&size=10
     */
    @GetMapping("/history")
    @RequireGreenhouseAccess
    public ApiResponse<PageResult<GrowthResponse>> history(
            @RequestParam Long greenhouseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(growthService.getHistory(greenhouseId, page, size));
    }

    /**
     * 截帧图片列表（分页）
     * GET /api/v1/growth/images?greenhouseId=1&page=1&size=10
     */
    @GetMapping("/images")
    @RequireGreenhouseAccess
    public ApiResponse<PageResult<GrowthImageResponse>> images(
            @RequestParam Long greenhouseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(growthService.getImages(greenhouseId, page, size));
    }
}
