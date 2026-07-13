package com.greenhouse.module.sensor.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.sensor.dto.*;
import com.greenhouse.module.sensor.service.SensorDataService;
import com.greenhouse.repository.GreenhouseRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 传感器数据 API
 * <p>
 * 提供实时数据、历史查询、多组对比、聚合统计、CSV 导出。
 * 路径前缀：/api/v1/sensors
 * </p>
 */
@RestController
@RequestMapping("/api/v1/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorDataService sensorDataService;
    private final GreenhouseRepository greenhouseRepository;

    /**
     * 实时数据
     * GET /api/v1/sensors/realtime?greenhouseId=1
     */
    @GetMapping("/realtime")
    public ApiResponse<SensorRealtimeResponse> realtime(@RequestParam Long greenhouseId) {
        String greenhouseName = greenhouseRepository.findById(greenhouseId)
                .map(gh -> gh.getName())
                .orElse("未知大棚");
        SensorRealtimeResponse data = sensorDataService.getRealtimeData(greenhouseId, greenhouseName);
        return ApiResponse.success(data);
    }

    /**
     * 历史数据（时间范围 + 聚合）
     * POST /api/v1/sensors/history?greenhouseId=1
     */
    @PostMapping("/history")
    public ApiResponse<List<SensorDataPoint>> history(
            @RequestParam Long greenhouseId,
            @Valid @RequestBody SensorHistoryRequest request) {
        List<SensorDataPoint> data = sensorDataService.getHistoryData(greenhouseId, request);
        return ApiResponse.success(data);
    }

    /**
     * 多组数据对比
     * POST /api/v1/sensors/compare
     */
    @PostMapping("/compare")
    public ApiResponse<SensorCompareResponse> compare(@RequestBody CompareRequest request) {
        SensorCompareResponse data = sensorDataService.getCompareData(
                request.getGreenhouseId(),
                request.getSensorType(),
                request.getDeviceIds(),
                request.getStartTime(),
                request.getEndTime()
        );
        return ApiResponse.success(data);
    }

    /**
     * 大棚聚合统计
     * GET /api/v1/sensors/aggregate?greenhouseId=1&sensorType=TEMPERATURE&startTime=...&endTime=...
     */
    @GetMapping("/aggregate")
    public ApiResponse<SensorAggregateResponse> aggregate(
            @RequestParam Long greenhouseId,
            @RequestParam String sensorType,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {
        SensorAggregateResponse data = sensorDataService.getAggregateData(
                greenhouseId, sensorType, startTime, endTime);
        return ApiResponse.success(data);
    }

    /**
     * 导出 CSV
     * GET /api/v1/sensors/export?greenhouseId=1&sensorType=TEMPERATURE&startTime=...&endTime=...
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam Long greenhouseId,
            @RequestParam String sensorType,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {
        String csv = sensorDataService.exportCsv(greenhouseId, sensorType, startTime, endTime);

        String filename = String.format("sensor_data_%d_%s.csv", greenhouseId, sensorType);
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
