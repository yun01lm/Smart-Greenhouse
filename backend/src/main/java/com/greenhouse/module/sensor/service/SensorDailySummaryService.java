package com.greenhouse.module.sensor.service;

import com.greenhouse.entity.SensorDailySummary;
import com.greenhouse.repository.SensorDailySummaryRepository;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传感器日汇总服务（R29）
 * <p>
 * 每天 00:05 将昨日 InfluxDB 原始数据按「大棚+设备+传感器类型+日期」聚合，
 * 日均/最小/最大/条数写入 MySQL 汇总表，供 7天/30天 历史趋势直接读取。
 * 应用启动时自动回填近 30 天（幂等，已存在的跳过）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDailySummaryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /** Flux 时区选项：聚合窗口按 Asia/Shanghai 自然日切分（注意：Flux 中 option 语句不加分号，否则编译报错） */
    private static final String LOCATION = "option location = {zone: \"Asia/Shanghai\"}\n";

    private final SensorDailySummaryRepository repository;
    private final QueryApi queryApi;
    private final InfluxDbConfigHelper configHelper;

    /** 每天 00:05 生成昨日汇总（昨日数据已完整） */
    @Scheduled(cron = "0 5 0 * * *")
    public void generateYesterday() {
        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);
        generateForRange(yesterday, yesterday);
    }

    /** 应用启动完成后回填近 30 天（幂等；InfluxDB 未就绪时仅记录日志，不影响主流程） */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        try {
            LocalDate today = LocalDate.now(ZONE);
            generateForRange(today.minusDays(30), today.minusDays(1));
        } catch (Exception e) {
            log.error("启动回填传感器日汇总失败（不影响主流程）: ", e);
        }
    }

    /**
     * 生成指定日期区间（闭区间）的日汇总，幂等：已存在的组合跳过
     */
    public void generateForRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) return;
        try {
            Map<String, SensorDailySummary> computed = queryInfluxDaily(start, end);
            int saved = 0;
            for (SensorDailySummary row : computed.values()) {
                if (repository.existsByGreenhouseIdAndDeviceIdAndSensorTypeAndStatDate(
                        row.getGreenhouseId(), row.getDeviceId(), row.getSensorType(), row.getStatDate())) {
                    continue;
                }
                repository.save(row);
                saved++;
            }
            log.info("传感器日汇总生成完成: {} ~ {}，计算 {} 条，新增 {} 条", start, end, computed.size(), saved);
        } catch (Exception e) {
            log.error("传感器日汇总生成失败 {} ~ {}: ", start, end, e);
        }
    }

    /** 一次性 Flux 查询整段时间内所有 大棚/设备/类型/天 的 mean/min/max/count */
    private Map<String, SensorDailySummary> queryInfluxDaily(LocalDate start, LocalDate end) {
        ZonedDateTime startZdt = start.atStartOfDay(ZONE);
        ZonedDateTime stopZdt = end.plusDays(1).atStartOfDay(ZONE);
        String base = LOCATION
                + "from(bucket: \"" + configHelper.getBucket() + "\") "
                + "|> range(start: " + startZdt.toInstant() + ", stop: " + stopZdt.toInstant() + ") "
                + "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") "
                + "|> filter(fn: (r) => r[\"_field\"] == \"value\") "
                + "|> aggregateWindow(every: 1d, fn: %s, createEmpty: false) "
                + "|> yield(name: \"%s\")";

        Map<String, SensorDailySummary> map = new HashMap<>();
        collect(base, "mean", map, "avg");
        collect(base, "min", map, "min");
        collect(base, "max", map, "max");
        collect(base, "count", map, "count");
        return map;
    }

    private void collect(String base, String fn, Map<String, SensorDailySummary> map, String field) {
        String flux = String.format(base, fn, fn);
        List<FluxTable> tables = queryApi.query(flux);
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String gh = (String) record.getValueByKey("greenhouse_id");
                String dev = (String) record.getValueByKey("device_id");
                String type = (String) record.getValueByKey("sensor_type");
                Object value = record.getValue();
                Instant time = record.getTime();
                if (gh == null || dev == null || type == null || value == null || time == null) continue;
                LocalDate date = time.atZone(ZONE).toLocalDate();
                String key = gh + "|" + dev + "|" + type + "|" + date;
                SensorDailySummary row = map.get(key);
                if (row == null) {
                    row = SensorDailySummary.builder()
                            .greenhouseId(Long.valueOf(gh))
                            .deviceId(Long.valueOf(dev))
                            .sensorType(type)
                            .statDate(date)
                            .build();
                    map.put(key, row);
                }
                switch (field) {
                    case "avg" -> row.setAvgValue((Double) value);
                    case "min" -> row.setMinValue((Double) value);
                    case "max" -> row.setMaxValue((Double) value);
                    case "count" -> row.setDataCount((Long) value);
                    default -> { }
                }
            }
        }
    }
}