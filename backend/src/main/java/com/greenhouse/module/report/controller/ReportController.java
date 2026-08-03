package com.greenhouse.module.report.controller;

import com.greenhouse.module.admin.service.AdminReportService;
import com.greenhouse.module.report.service.ReportAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 棚主/技术员数据导出 API（R8 新增）
 * <p>
 * 路径前缀：/api/v1/report，仅 OWNER / WORKER 角色可访问（SecurityConfig 统一收口）。
 * 细粒度校验由 ReportAccessService 负责：OWNER 仅导出本人大棚，WORKER 仅导出被授权大棚。
 * Excel 生成逻辑复用 AdminReportService，避免重复实现。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final AdminReportService reportService;
    private final ReportAccessService accessService;

    /**
     * 导出传感器历史数据
     * GET /api/v1/report/sensors?greenhouseId=&sensorType=&startTime=&endTime=
     */
    @GetMapping("/sensors")
    public ResponseEntity<byte[]> exportSensors(
            @RequestParam Long greenhouseId,
            @RequestParam String sensorType,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        Long userId = currentUserId();
        accessService.assertExportAccess(userId, greenhouseId);

        // 默认最近 7 天
        if (endTime == null || endTime <= 0) endTime = System.currentTimeMillis();
        if (startTime == null || startTime <= 0) startTime = endTime - 7 * 24 * 60 * 60 * 1000L;

        log.info("[REPORT] 导出传感器历史数据: userId={}, greenhouseId={}, sensorType={}",
                userId, greenhouseId, sensorType);
        byte[] data = reportService.exportSensorHistory(greenhouseId, sensorType, startTime, endTime);
        return excelResponse(data, "传感器数据_" + today() + ".xlsx");
    }

    /**
     * 导出预警记录
     * GET /api/v1/report/alerts?greenhouseId=&level=&startTime=&endTime=
     */
    @GetMapping("/alerts")
    public ResponseEntity<byte[]> exportAlerts(
            @RequestParam Long greenhouseId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        Long userId = currentUserId();
        accessService.assertExportAccess(userId, greenhouseId);

        if (endTime == null || endTime <= 0) endTime = System.currentTimeMillis();
        if (startTime == null || startTime <= 0) startTime = endTime - 30L * 24 * 60 * 60 * 1000;

        log.info("[REPORT] 导出预警记录: userId={}, greenhouseId={}, level={}", userId, greenhouseId, level);
        byte[] data = reportService.exportAlerts(greenhouseId, level, startTime, endTime);
        return excelResponse(data, "预警记录_" + today() + ".xlsx");
    }

    /**
     * 导出设备控制日志
     * GET /api/v1/report/controls?greenhouseId=&startTime=&endTime=
     */
    @GetMapping("/controls")
    public ResponseEntity<byte[]> exportControls(
            @RequestParam Long greenhouseId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        Long userId = currentUserId();
        accessService.assertExportAccess(userId, greenhouseId);

        if (endTime == null || endTime <= 0) endTime = System.currentTimeMillis();
        if (startTime == null || startTime <= 0) startTime = endTime - 30L * 24 * 60 * 60 * 1000;

        log.info("[REPORT] 导出设备控制日志: userId={}, greenhouseId={}", userId, greenhouseId);
        byte[] data = reportService.exportControlLogs(greenhouseId, startTime, endTime);
        return excelResponse(data, "控制日志_" + today() + ".xlsx");
    }

    /**
     * 导出健康评分记录
     * GET /api/v1/report/health?greenhouseId=&startTime=&endTime=
     */
    @GetMapping("/health")
    public ResponseEntity<byte[]> exportHealth(
            @RequestParam Long greenhouseId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {

        Long userId = currentUserId();
        accessService.assertExportAccess(userId, greenhouseId);

        log.info("[REPORT] 导出健康评分记录: userId={}, greenhouseId={}", userId, greenhouseId);
        byte[] data = reportService.exportHealthScores(greenhouseId, startTime, endTime);
        return excelResponse(data, "健康评分_" + today() + ".xlsx");
    }

    // ===== 辅助 =====

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

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