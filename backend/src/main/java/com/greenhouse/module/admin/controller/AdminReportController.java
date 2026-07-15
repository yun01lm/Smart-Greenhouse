package com.greenhouse.module.admin.controller;

import com.greenhouse.module.admin.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 管理员数据导出 API
 * <p>
 * 路径前缀：/api/v1/admin/report，仅 ADMIN 角色可访问。
 * 所有端点返回 Excel（.xlsx）文件流。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService reportService;

    /**
     * 导出传感器历史数据
     * GET /api/v1/admin/report/sensors?greenhouseId=&sensorType=&startTime=&endTime=
     */
    @GetMapping("/sensors")
    public ResponseEntity<byte[]> exportSensors(
            @RequestParam Long greenhouseId,
            @RequestParam String sensorType,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        // 默认最近 7 天
        if (endTime == null || endTime <= 0) endTime = System.currentTimeMillis();
        if (startTime == null || startTime <= 0) startTime = endTime - 7 * 24 * 60 * 60 * 1000L;

        log.info("[ADMIN] 导出传感器历史数据: greenhouseId={}, sensorType={}", greenhouseId, sensorType);
        byte[] data = reportService.exportSensorHistory(greenhouseId, sensorType, startTime, endTime);
        return excelResponse(data, "传感器数据_" + today() + ".xlsx");
    }

    /**
     * 导出预警记录
     * GET /api/v1/admin/report/alerts?greenhouseId=&level=&startTime=&endTime=
     */
    @GetMapping("/alerts")
    public ResponseEntity<byte[]> exportAlerts(
            @RequestParam Long greenhouseId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        if (endTime == null || endTime <= 0) endTime = System.currentTimeMillis();
        if (startTime == null || startTime <= 0) startTime = endTime - 30L * 24 * 60 * 60 * 1000;

        log.info("[ADMIN] 导出预警记录: greenhouseId={}, level={}", greenhouseId, level);
        byte[] data = reportService.exportAlerts(greenhouseId, level, startTime, endTime);
        return excelResponse(data, "预警记录_" + today() + ".xlsx");
    }

    /**
     * 导出设备控制日志
     * GET /api/v1/admin/report/controls?greenhouseId=&startTime=&endTime=
     */
    @GetMapping("/controls")
    public ResponseEntity<byte[]> exportControls(
            @RequestParam Long greenhouseId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        if (endTime == null || endTime <= 0) endTime = System.currentTimeMillis();
        if (startTime == null || startTime <= 0) startTime = endTime - 30L * 24 * 60 * 60 * 1000;

        log.info("[ADMIN] 导出设备控制日志: greenhouseId={}", greenhouseId);
        byte[] data = reportService.exportControlLogs(greenhouseId, startTime, endTime);
        return excelResponse(data, "控制日志_" + today() + ".xlsx");
    }

    /**
     * 导出健康评分记录
     * GET /api/v1/admin/report/health?greenhouseId=&startTime=&endTime=
     */
    @GetMapping("/health")
    public ResponseEntity<byte[]> exportHealth(
            @RequestParam Long greenhouseId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        log.info("[ADMIN] 导出健康评分记录: greenhouseId={}", greenhouseId);
        byte[] data = reportService.exportHealthScores(greenhouseId, startTime, endTime);
        return excelResponse(data, "健康评分_" + today() + ".xlsx");
    }

    // ===== 辅助 =====

    private ResponseEntity<byte[]> excelResponse(byte[] data, String filename) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}
