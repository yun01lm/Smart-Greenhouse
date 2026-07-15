package com.greenhouse.module.admin.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.*;
import com.greenhouse.module.sensor.dto.SensorDataPoint;
import com.greenhouse.module.sensor.service.SensorDataService;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员报表导出服务
 * <p>
 * 支持 4 种数据类型导出为 Excel（.xlsx）：
 * 1. 传感器历史数据
 * 2. 预警记录
 * 3. 设备控制日志
 * 4. 健康评分记录
 * </p>
 * <p>
 * 设计决策（记录于 TASK-G06.md）：
 * - 选择服务端报表方案（Apache POI），而非前端导出。原因：支持大数据量、格式统一、
 *   多 Sheet 报表、不受浏览器内存限制。
 * - 当前仅支持 4 种类型作为实验版本，后续按需扩展。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final SensorDataService sensorDataService;
    private final AlertRepository alertRepository;
    private final ControlLogRepository controlLogRepository;
    private final HealthAssessmentRepository healthAssessmentRepository;
    private final DeviceRepository deviceRepository;
    private final GreenhouseRepository greenhouseRepository;

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ===== 1. 传感器历史数据导出 =====

    /**
     * 导出传感器历史数据为 Excel
     *
     * @param greenhouseId 大棚 ID
     * @param sensorType   传感器类型（如 TEMP、HUMIDITY）
     * @param startTime    开始时间（epoch 毫秒）
     * @param endTime      结束时间（epoch 毫秒）
     * @return Excel 文件字节数组
     */
    public byte[] exportSensorHistory(Long greenhouseId, String sensorType,
                                      Long startTime, Long endTime) {
        List<SensorDataPoint> data = sensorDataService.getHistoryData(greenhouseId,
                com.greenhouse.module.sensor.dto.SensorHistoryRequest.builder()
                        .sensorType(sensorType)
                        .startTime(startTime)
                        .endTime(endTime)
                        .interval("5m")
                        .build());

        // 填充设备名称
        Map<Long, String> deviceNameMap = deviceRepository.findByGreenhouseId(greenhouseId)
                .stream().collect(Collectors.toMap(Device::getId, Device::getName, (a, b) -> a));

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("传感器历史数据");
            createHeader(wb, sheet, new String[]{"时间", "设备ID", "设备名称", "传感器类型", "数值"});

            int rowIdx = 1;
            for (SensorDataPoint point : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(formatInstant(point.getTimestamp()));
                row.createCell(1).setCellValue(point.getDeviceId() != null ? point.getDeviceId() : 0);
                row.createCell(2).setCellValue(deviceNameMap.getOrDefault(point.getDeviceId(), "未知设备"));
                row.createCell(3).setCellValue(point.getSensorType());
                row.createCell(4).setCellValue(point.getValue() != null ? point.getValue() : 0);
            }

            autoSizeColumns(sheet, 5);
            return toByteArray(wb);
        } catch (IOException e) {
            log.error("导出传感器历史数据失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 生成失败");
        }
    }

    // ===== 2. 预警记录导出 =====

    /**
     * 导出预警记录为 Excel
     */
    public byte[] exportAlerts(Long greenhouseId, String level,
                                Long startTime, Long endTime) {
        Alert.AlertLevel alertLevel = (level != null && !level.isEmpty())
                ? Alert.AlertLevel.valueOf(level) : null;

        LocalDateTime start = toLocalDateTime(startTime);
        LocalDateTime end = toLocalDateTime(endTime);

        // 按时间范围查询（全量，不分页）
        List<Alert> alerts;
        if (alertLevel != null) {
            alerts = alertRepository.findByGreenhouseIdAndLevelOrderByCreatedAtDesc(
                    greenhouseId, alertLevel, org.springframework.data.domain.Pageable.unpaged()).getContent();
        } else {
            alerts = alertRepository.findByGreenhouseIdOrderByCreatedAtDesc(
                    greenhouseId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        }

        // 时间范围过滤
        if (start != null) {
            alerts = alerts.stream().filter(a -> !a.getCreatedAt().isBefore(start)).toList();
        }
        if (end != null) {
            alerts = alerts.stream().filter(a -> !a.getCreatedAt().isAfter(end)).toList();
        }

        String ghName = greenhouseRepository.findById(greenhouseId)
                .map(Greenhouse::getName).orElse("大棚 #" + greenhouseId);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("预警记录");
            createHeader(wb, sheet, new String[]{"ID", "大棚", "级别", "标题", "内容", "传感器类型",
                    "传感器数值", "已读", "创建时间"});

            int rowIdx = 1;
            for (Alert alert : alerts) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(alert.getId());
                row.createCell(1).setCellValue(ghName);
                row.createCell(2).setCellValue(levelLabel(alert.getLevel()));
                row.createCell(3).setCellValue(alert.getTitle() != null ? alert.getTitle() : "");
                row.createCell(4).setCellValue(alert.getContent() != null ? alert.getContent() : "");
                row.createCell(5).setCellValue(alert.getSensorType() != null ? alert.getSensorType() : "");
                row.createCell(6).setCellValue(alert.getSensorValue() != null
                        ? alert.getSensorValue().doubleValue() : 0.0);
                row.createCell(7).setCellValue(alert.getReadStatus() != null && alert.getReadStatus() ? "是" : "否");
                row.createCell(8).setCellValue(formatDateTime(alert.getCreatedAt()));
            }

            autoSizeColumns(sheet, 9);
            return toByteArray(wb);
        } catch (IOException e) {
            log.error("导出预警记录失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 生成失败");
        }
    }

    // ===== 3. 设备控制日志导出 =====

    /**
     * 导出设备控制日志为 Excel
     */
    public byte[] exportControlLogs(Long greenhouseId, Long startTime, Long endTime) {
        // 获取该大棚下所有设备 ID
        List<Long> deviceIds = deviceRepository.findByGreenhouseId(greenhouseId)
                .stream().map(Device::getId).toList();

        if (deviceIds.isEmpty()) {
            return createEmptyWorkbook("设备控制日志",
                    new String[]{"ID", "设备ID", "设备名称", "操作人ID", "动作", "来源", "成功", "失败原因", "时间"});
        }

        List<ControlLog> logs = controlLogRepository
                .findByDeviceIdInOrderByCreatedAtDesc(deviceIds,
                        org.springframework.data.domain.Pageable.unpaged()).getContent();

        LocalDateTime start = toLocalDateTime(startTime);
        LocalDateTime end = toLocalDateTime(endTime);
        if (start != null) {
            logs = logs.stream().filter(l -> !l.getCreatedAt().isBefore(start)).toList();
        }
        if (end != null) {
            logs = logs.stream().filter(l -> !l.getCreatedAt().isAfter(end)).toList();
        }

        Map<Long, String> deviceNameMap = deviceRepository.findByGreenhouseId(greenhouseId)
                .stream().collect(Collectors.toMap(Device::getId, Device::getName, (a, b) -> a));

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("设备控制日志");
            createHeader(wb, sheet, new String[]{"ID", "设备ID", "设备名称", "操作人ID", "动作",
                    "来源", "成功", "失败原因", "时间"});

            int rowIdx = 1;
            for (ControlLog log : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getDeviceId());
                row.createCell(2).setCellValue(deviceNameMap.getOrDefault(log.getDeviceId(), "未知设备"));
                row.createCell(3).setCellValue(log.getUserId() != null ? log.getUserId() : 0);
                row.createCell(4).setCellValue(log.getAction());
                row.createCell(5).setCellValue(sourceLabel(log.getSource()));
                row.createCell(6).setCellValue(log.getSuccess() != null && log.getSuccess() ? "成功" : "失败");
                row.createCell(7).setCellValue(log.getFailReason() != null ? log.getFailReason() : "");
                row.createCell(8).setCellValue(formatDateTime(log.getCreatedAt()));
            }

            autoSizeColumns(sheet, 9);
            return toByteArray(wb);
        } catch (IOException e) {
            log.error("导出控制日志失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 生成失败");
        }
    }

    // ===== 4. 健康评分记录导出 =====

    /**
     * 导出健康评分记录为 Excel
     */
    public byte[] exportHealthScores(Long greenhouseId, Long startTime, Long endTime) {
        LocalDateTime start = toLocalDateTime(startTime);
        LocalDateTime end = toLocalDateTime(endTime);

        if (start == null) start = LocalDateTime.now().minusDays(30);
        if (end == null) end = LocalDateTime.now();

        List<HealthAssessment> assessments = healthAssessmentRepository
                .findByGreenhouseIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        greenhouseId, start, end, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        String ghName = greenhouseRepository.findById(greenhouseId)
                .map(Greenhouse::getName).orElse("大棚 #" + greenhouseId);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("健康评分记录");
            createHeader(wb, sheet, new String[]{"ID", "大棚", "综合评分", "等级", "环境分",
                    "视觉分", "天气修正", "评估时间"});

            int rowIdx = 1;
            for (HealthAssessment assessment : assessments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(assessment.getId());
                row.createCell(1).setCellValue(ghName);
                double score = assessment.getOverallScore() != null
                        ? assessment.getOverallScore().doubleValue() : 0.0;
                row.createCell(2).setCellValue(score);
                row.createCell(3).setCellValue(HealthAssessment.ScoreLevel.fromScore(score).getLabel());
                row.createCell(4).setCellValue(assessment.getEnvScore() != null
                        ? assessment.getEnvScore().doubleValue() : 0.0);
                row.createCell(5).setCellValue(assessment.getVisualScore() != null
                        ? assessment.getVisualScore().doubleValue() : 0.0);
                row.createCell(6).setCellValue(assessment.getWeatherFactor() != null
                        ? assessment.getWeatherFactor().doubleValue() : 1.0);
                row.createCell(7).setCellValue(formatDateTime(assessment.getCreatedAt()));
            }

            autoSizeColumns(sheet, 8);
            return toByteArray(wb);
        } catch (IOException e) {
            log.error("导出健康评分记录失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 生成失败");
        }
    }

    // ===== 辅助方法 =====

    /**
     * 创建表头行（加粗 + 灰色背景）
     */
    private void createHeader(Workbook wb, Sheet sheet, String[] titles) {
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        Row header = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * 自动调整列宽
     */
    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // 限制最大宽度 50 字符
            int width = Math.min(sheet.getColumnWidth(i), 50 * 256);
            sheet.setColumnWidth(i, width);
        }
    }

    /**
     * 将 Workbook 转换为字节数组
     */
    private byte[] toByteArray(Workbook wb) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        return bos.toByteArray();
    }

    /**
     * 创建空工作簿（无数据时）
     */
    private byte[] createEmptyWorkbook(String sheetName, String[] headers) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);
            createHeader(wb, sheet, headers);
            sheet.createRow(1).createCell(0).setCellValue("（无数据）");
            return toByteArray(wb);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Excel 生成失败");
        }
    }

    private String formatInstant(Instant instant) {
        if (instant == null) return "";
        return LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"))
                .format(DT_FORMAT);
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DT_FORMAT);
    }

    private LocalDateTime toLocalDateTime(Long epochMilli) {
        if (epochMilli == null || epochMilli <= 0) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.of("Asia/Shanghai"));
    }

    private String levelLabel(Alert.AlertLevel level) {
        if (level == null) return "未知";
        return switch (level) {
            case INFO -> "提示";
            case WARNING -> "警告";
            case CRITICAL -> "严重";
        };
    }

    private String sourceLabel(String source) {
        if (source == null) return "未知";
        return switch (source) {
            case "MANUAL" -> "手动控制";
            case "SCENE" -> "场景联动";
            case "ALERT" -> "预警触发";
            default -> source;
        };
    }
}
