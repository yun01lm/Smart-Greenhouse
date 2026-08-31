package com.greenhouse.module.sensor.service;

import com.greenhouse.entity.Device;
import com.greenhouse.repository.DeviceRepository;
import com.greenhouse.module.sensor.dto.*;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.SensorDailySummary;
import com.greenhouse.repository.SensorDailySummaryRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 传感器时序数据服务
 * <p>
 * 负责传感器数据的 InfluxDB 写入和查询。
 * measurement: sensor_data
 * tags: greenhouse_id, device_id, sensor_type
 * fields: value
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataService {

    private final WriteApiBlocking writeApi;
    private final QueryApi queryApi;
    private final InfluxDBClient influxDBClient;
    private final DeviceRepository deviceRepository;
    private final InfluxDbConfigHelper configHelper;
    private final TrendPredictor trendPredictor;
    private final SensorDailySummaryRepository dailySummaryRepository;

    /**
     * 删除某大棚的全部 InfluxDB 时序数据（级联删除大棚时调用）
     */
    public void deleteGreenhouseData(Long greenhouseId) {
        try {
            var deleteApi = influxDBClient.getDeleteApi();
            // 删除从 1970 年到现在的全部该大棚数据
            deleteApi.delete(
                    Instant.ofEpochMilli(0).atOffset(java.time.ZoneOffset.UTC),
                    Instant.now().atOffset(java.time.ZoneOffset.UTC),
                    String.format("_measurement=\"sensor_data\" AND greenhouse_id=\"%d\"", greenhouseId),
                    configHelper.getBucket(),
                    configHelper.getOrg());
            log.info("InfluxDB 时序数据已清理: greenhouseId={}", greenhouseId);
        } catch (Exception e) {
            log.warn("InfluxDB 时序数据清理失败(忽略继续): greenhouseId={}, error={}", greenhouseId, e.getMessage());
        }
    }

    /**
     * 写入传感器数据到 InfluxDB
     */
    public void writeData(Long greenhouseId, Long deviceId, String sensorType,
                          Double value, long timestampMs) {
        com.influxdb.client.domain.WritePrecision precision =
                com.influxdb.client.domain.WritePrecision.MS;

        com.influxdb.client.write.Point point = com.influxdb.client.write.Point
                .measurement("sensor_data")
                .addTag("greenhouse_id", String.valueOf(greenhouseId))
                .addTag("device_id", String.valueOf(deviceId))
                .addTag("sensor_type", sensorType)
                .addField("value", value)
                .time(timestampMs, precision);

        writeApi.writePoint(point);
    }

    /**
     * 更新设备状态（收到 MQTT 数据时调用）
     * @return 设备名称
     */
    public String updateDeviceStatus(Long deviceId, Double value) {
        return deviceRepository.findById(deviceId)
                .map(device -> {
                    device.setStatus(Device.DeviceStatus.ONLINE);
                    device.setLastValue(String.format("%.2f", value));
                    device.setLastDataTime(java.time.LocalDateTime.now());
                    deviceRepository.save(device);
                    return device.getName();
                })
                .orElse("未知设备");
    }

    /**
     * 仅更新设备在线状态（控制器心跳专用）
     */
    public void updateDeviceOnline(Long deviceId) {
        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setStatus(Device.DeviceStatus.ONLINE);
            device.setLastDataTime(java.time.LocalDateTime.now());
            deviceRepository.save(device);
        });
    }

    /**
     * 查询实时数据（大棚下所有传感器最新值）
     */    @Cacheable(value = "sensorRealtime", key = "#greenhouseId")
    public SensorRealtimeResponse getRealtimeData(Long greenhouseId, String greenhouseName) {
        // 查询过去 5 分钟内所有传感器的最新数据
        String flux = String.format(
                "from(bucket: \"sensor_data\") " +
                        "|> range(start: -5m) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                        "|> filter(fn: (r) => r[\"greenhouse_id\"] == \"%d\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"value\") " +
                        "|> last()",
                greenhouseId
        );

        List<FluxTable> tables = queryApi.query(flux);
        Map<String, List<SensorDataPoint>> dataByType = new LinkedHashMap<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String sensorType = (String) record.getValueByKey("sensor_type");
                Long deviceId = Long.valueOf((String) record.getValueByKey("device_id"));
                Double value = (Double) record.getValue();
                Instant time = record.getTime();

                SensorDataPoint point = SensorDataPoint.builder()
                        .greenhouseId(greenhouseId)
                        .deviceId(deviceId)
                        .sensorType(sensorType)
                        .value(value)
                        .timestamp(time)
                        .deviceName(getDeviceName(deviceId))
                        .build();

                dataByType.computeIfAbsent(sensorType, k -> new ArrayList<>()).add(point);
            }
        }

        return SensorRealtimeResponse.builder()
                .greenhouseId(greenhouseId)
                .greenhouseName(greenhouseName)
                .dataByType(dataByType)
                .build();
    }

    /**
     * 查询历史数据（时间范围 + 聚合间隔）
     */
    public List<SensorDataPoint> getHistoryData(Long greenhouseId, SensorHistoryRequest request) {
        validateSensorType(request.getSensorType());
        // R29：日粒度查询（7天/30天趋势图）优先读 MySQL 日汇总表，避免实时扫描 InfluxDB 原始数据
        if ("1d".equals(request.getInterval())) {
            return getDailyHistory(greenhouseId, request);
        }
        Instant start = Instant.ofEpochMilli(request.getStartTime());
        Instant end = Instant.ofEpochMilli(request.getEndTime());

        String flux = String.format(
                "from(bucket: \"sensor_data\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                        "|> filter(fn: (r) => r[\"greenhouse_id\"] == \"%d\") " +
                        "|> filter(fn: (r) => r[\"sensor_type\"] == \"%s\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"value\") " +
                        "|> aggregateWindow(every: %s, fn: mean, createEmpty: false) " +
                        "|> yield(name: \"mean\")",
                start.toString(), end.toString(), greenhouseId,
                request.getSensorType(), request.getInterval()
        );

        List<FluxTable> tables = queryApi.query(flux);
        List<SensorDataPoint> result = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Long deviceId = record.getValueByKey("device_id") != null
                        ? Long.valueOf((String) record.getValueByKey("device_id")) : null;
                Double value = record.getValue() != null ? (Double) record.getValue() : null;
                Instant time = record.getTime();

                if (deviceId != null && value != null) {
                    result.add(SensorDataPoint.builder()
                            .greenhouseId(greenhouseId)
                            .deviceId(deviceId)
                            .sensorType(request.getSensorType())
                            .value(value)
                            .timestamp(time)
                            .deviceName(getDeviceName(deviceId))
                            .build());
                }
            }
        }

        // 按时间排序
        result.sort(Comparator.comparing(SensorDataPoint::getTimestamp));
        return result;
    }

    /**
     * 环境参数短期预测（第一阶段统计方法，后续可替换 LSTM Provider）
     */
    public ForecastResponse getForecast(Long greenhouseId, String sensorType, int steps, int intervalMinutes) {
        validateSensorType(sensorType);
        long end = System.currentTimeMillis();
        long start = end - 24L * 3600 * 1000;
        SensorHistoryRequest request = SensorHistoryRequest.builder()
                .sensorType(sensorType)
                .startTime(start)
                .endTime(end)
                .interval("1h")
                .build();
        List<SensorDataPoint> history = getHistoryData(greenhouseId, request);
        List<ForecastPoint> points = trendPredictor.predict(sensorType, history, steps, intervalMinutes * 60000L);
        return ForecastResponse.builder()
                .greenhouseId(greenhouseId)
                .sensorType(sensorType)
                .intervalMinutes(intervalMinutes)
                .points(points)
                .build();
    }

    /**
     * 多组传感器数据对比
     */
    public SensorCompareResponse getCompareData(Long greenhouseId, String sensorType,
                                                  List<Long> deviceIds,
                                                  Long startTime, Long endTime) {
        validateSensorType(sensorType);
        Instant start = Instant.ofEpochMilli(startTime);
        Instant end = Instant.ofEpochMilli(endTime);

        String deviceFilter = deviceIds.stream()
                .map(id -> "r[\"device_id\"] == \"" + id + "\"")
                .collect(Collectors.joining(" or "));

        String flux = String.format(
                "from(bucket: \"sensor_data\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                        "|> filter(fn: (r) => r[\"greenhouse_id\"] == \"%d\") " +
                        "|> filter(fn: (r) => r[\"sensor_type\"] == \"%s\") " +
                        "|> filter(fn: (r) => %s) " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"value\") " +
                        "|> aggregateWindow(every: 5m, fn: mean, createEmpty: false) " +
                        "|> yield(name: \"compare\")",
                start.toString(), end.toString(), greenhouseId, sensorType, deviceFilter
        );

        List<FluxTable> tables = queryApi.query(flux);

        Map<Long, List<SensorDataPoint>> dataByDevice = new LinkedHashMap<>();
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Long deviceId = Long.valueOf((String) record.getValueByKey("device_id"));
                Double value = (Double) record.getValue();
                Instant time = record.getTime();

                dataByDevice.computeIfAbsent(deviceId, k -> new ArrayList<>())
                        .add(SensorDataPoint.builder()
                                .deviceId(deviceId)
                                .sensorType(sensorType)
                                .value(value)
                                .timestamp(time)
                                .build());
            }
        }

        List<SensorCompareResponse.DeviceSeries> series = dataByDevice.entrySet().stream()
                .map(entry -> {
                    entry.getValue().sort(Comparator.comparing(SensorDataPoint::getTimestamp));
                    return SensorCompareResponse.DeviceSeries.builder()
                            .deviceId(entry.getKey())
                            .deviceName(getDeviceName(entry.getKey()))
                            .dataPoints(entry.getValue())
                            .build();
                })
                .collect(Collectors.toList());

        return SensorCompareResponse.builder()
                .greenhouseId(greenhouseId)
                .sensorType(sensorType)
                .deviceIds(deviceIds)
                .series(series)
                .build();
    }

    /**
     * 大棚聚合统计（指定传感器类型的平均/最高/最低/最新）
     */
    public SensorAggregateResponse getAggregateData(Long greenhouseId, String sensorType,
                                                     Long startTime, Long endTime) {
        validateSensorType(sensorType);
        Instant start = Instant.ofEpochMilli(startTime);
        Instant end = Instant.ofEpochMilli(endTime);

        String flux = String.format(
                "from(bucket: \"sensor_data\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                        "|> filter(fn: (r) => r[\"greenhouse_id\"] == \"%d\") " +
                        "|> filter(fn: (r) => r[\"sensor_type\"] == \"%s\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"value\") " +
                        "|> mean()",
                start.toString(), end.toString(), greenhouseId, sensorType
        );

        List<FluxTable> meanTables = queryApi.query(flux);
        Double avgValue = null;
        if (!meanTables.isEmpty() && !meanTables.get(0).getRecords().isEmpty()) {
            avgValue = (Double) meanTables.get(0).getRecords().get(0).getValue();
        }

        // 最大值
        String maxFlux = flux.replace("mean()", "max()");
        List<FluxTable> maxTables = queryApi.query(maxFlux);
        Double maxValue = null;
        if (!maxTables.isEmpty() && !maxTables.get(0).getRecords().isEmpty()) {
            maxValue = (Double) maxTables.get(0).getRecords().get(0).getValue();
        }

        // 最小值
        String minFlux = flux.replace("mean()", "min()");
        List<FluxTable> minTables = queryApi.query(minFlux);
        Double minValue = null;
        if (!minTables.isEmpty() && !minTables.get(0).getRecords().isEmpty()) {
            minValue = (Double) minTables.get(0).getRecords().get(0).getValue();
        }

        // 最新值 + 数据条数
        String lastFlux = String.format(
                "from(bucket: \"sensor_data\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                        "|> filter(fn: (r) => r[\"greenhouse_id\"] == \"%d\") " +
                        "|> filter(fn: (r) => r[\"sensor_type\"] == \"%s\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"value\") " +
                        "|> last()",
                start.toString(), end.toString(), greenhouseId, sensorType
        );
        List<FluxTable> lastTables = queryApi.query(lastFlux);
        Double latestValue = null;
        if (!lastTables.isEmpty() && !lastTables.get(0).getRecords().isEmpty()) {
            latestValue = (Double) lastTables.get(0).getRecords().get(0).getValue();
        }

        // 数据条数
        String countFlux = String.format(
                "from(bucket: \"sensor_data\") " +
                        "|> range(start: %s, stop: %s) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                        "|> filter(fn: (r) => r[\"greenhouse_id\"] == \"%d\") " +
                        "|> filter(fn: (r) => r[\"sensor_type\"] == \"%s\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"value\") " +
                        "|> count()",
                start.toString(), end.toString(), greenhouseId, sensorType
        );
        List<FluxTable> countTables = queryApi.query(countFlux);
        Long dataCount = null;
        if (!countTables.isEmpty() && !countTables.get(0).getRecords().isEmpty()) {
            dataCount = (Long) countTables.get(0).getRecords().get(0).getValue();
        }

        return SensorAggregateResponse.builder()
                .greenhouseId(greenhouseId)
                .sensorType(sensorType)
                .avgValue(avgValue)
                .maxValue(maxValue)
                .minValue(minValue)
                .latestValue(latestValue)
                .dataCount(dataCount)
                .build();
    }

    /**
     * 导出 CSV 数据
     * <p>
     * 返回 CSV 格式字符串：timestamp,device_id,sensor_type,value
     * </p>
     */
    public String exportCsv(Long greenhouseId, String sensorType, Long startTime, Long endTime) {
        SensorHistoryRequest request = SensorHistoryRequest.builder()
                .sensorType(sensorType)
                .startTime(startTime)
                .endTime(endTime)
                .interval("1m")
                .build();

        List<SensorDataPoint> data = getHistoryData(greenhouseId, request);

        StringBuilder csv = new StringBuilder();
        csv.append("timestamp,device_id,sensor_type,value\n");
        for (SensorDataPoint point : data) {
            csv.append(String.format("%s,%d,%s,%.2f\n",
                    point.getTimestamp().toString(),
                    point.getDeviceId(),
                    point.getSensorType(),
                    point.getValue()));
        }

        return csv.toString();
    }

    // ===== R29：传感器类型白名单 + 日汇总读取 =====

    /** 支持的传感器类型白名单（对应 Device.SensorType 枚举，防未知类型请求打 InfluxDB） */
    public static final java.util.Set<String> SUPPORTED_SENSOR_TYPES =
            java.util.Arrays.stream(Device.SensorType.values())
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

    /** 传感器数据统计时区（与日汇总表 stat_date 保持一致） */
    public static final ZoneId SENSOR_ZONE = ZoneId.of("Asia/Shanghai");

    /** 校验传感器类型白名单 */
    private void validateSensorType(String sensorType) {
        if (sensorType == null || !SUPPORTED_SENSOR_TYPES.contains(sensorType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的传感器类型: " + sensorType);
        }
    }

    /**
     * 日粒度历史读取：完整日期优先读 MySQL 日汇总表，今日与缺日回退 InfluxDB 原始聚合
     */
    private List<SensorDataPoint> getDailyHistory(Long greenhouseId, SensorHistoryRequest request) {
        LocalDate startDate = Instant.ofEpochMilli(request.getStartTime()).atZone(SENSOR_ZONE).toLocalDate();
        LocalDate endDate = Instant.ofEpochMilli(request.getEndTime()).atZone(SENSOR_ZONE).toLocalDate();
        LocalDate today = LocalDate.now(SENSOR_ZONE);
        LocalDate summaryEnd = today.minusDays(1);

        Map<LocalDate, List<Double>> dayAvgs = new TreeMap<>();
        Map<LocalDate, Double> dayMin = new HashMap<>();
        Map<LocalDate, Double> dayMax = new HashMap<>();
        Map<LocalDate, Long> dayCount = new HashMap<>();

        if (!startDate.isAfter(summaryEnd)) {
            List<SensorDailySummary> rows = dailySummaryRepository
                    .findByGreenhouseIdAndSensorTypeAndStatDateBetween(
                            greenhouseId, request.getSensorType(), startDate, summaryEnd);
            for (SensorDailySummary row : rows) {
                if (row.getAvgValue() == null) continue;
                dayAvgs.computeIfAbsent(row.getStatDate(), k -> new ArrayList<>()).add(row.getAvgValue());
                if (row.getMinValue() != null) dayMin.merge(row.getStatDate(), row.getMinValue(), Math::min);
                if (row.getMaxValue() != null) dayMax.merge(row.getStatDate(), row.getMaxValue(), Math::max);
                if (row.getDataCount() != null) dayCount.merge(row.getStatDate(), row.getDataCount(), Long::sum);
            }
        }

        List<LocalDate> missing = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            if (d.isAfter(summaryEnd) || !dayAvgs.containsKey(d)) {
                missing.add(d);
            }
        }
        if (!missing.isEmpty()) {
            Map<LocalDate, DailyStat> influx = queryDailyStatsFromInflux(
                    greenhouseId, request.getSensorType(), missing.get(0), missing.get(missing.size() - 1));
            for (DailyStat s : influx.values()) {
                if (s.getAvg() == null) continue;
                dayAvgs.put(s.getDate(), new ArrayList<>(List.of(s.getAvg())));
                if (s.getMin() != null) dayMin.merge(s.getDate(), s.getMin(), Math::min);
                if (s.getMax() != null) dayMax.merge(s.getDate(), s.getMax(), Math::max);
                if (s.getCount() != null) dayCount.merge(s.getDate(), s.getCount(), Long::sum);
            }
        }

        List<SensorDataPoint> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Double>> e : dayAvgs.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
            if (Double.isNaN(avg)) continue;
            result.add(SensorDataPoint.builder()
                    .greenhouseId(greenhouseId)
                    .sensorType(request.getSensorType())
                    .value(Math.round(avg * 100.0) / 100.0)
                    .timestamp(e.getKey().atStartOfDay(SENSOR_ZONE).toInstant())
                    .build());
        }
        result.sort(Comparator.comparing(SensorDataPoint::getTimestamp));
        return result;
    }

    /**
     * 从 InfluxDB 按天聚合（Asia/Shanghai 日界）获取日均/最小/最大/条数
     */
    public Map<LocalDate, DailyStat> queryDailyStatsFromInflux(
            Long greenhouseId, String sensorType, LocalDate start, LocalDate end) {
        ZonedDateTime startZdt = start.atStartOfDay(SENSOR_ZONE);
        ZonedDateTime stopZdt = end.plusDays(1).atStartOfDay(SENSOR_ZONE);
        String template = "option location = {zone: \"Asia/Shanghai\"};\n"
                + "from(bucket: \"%s\") "
                + "|> range(start: %s, stop: %s) "
                + "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") "
                + "|> filter(fn: (r) => r[\"greenhouse_id\"] == \"%d\") "
                + "|> filter(fn: (r) => r[\"sensor_type\"] == \"%s\") "
                + "|> filter(fn: (r) => r[\"_field\"] == \"value\") "
                + "|> aggregateWindow(every: 1d, fn: %s, createEmpty: false) "
                + "|> yield(name: \"%s\")";

        Map<LocalDate, DayAcc> acc = new TreeMap<>();
        collectDailyAgg(template, greenhouseId, sensorType, startZdt, stopZdt, "mean", acc, "avg");
        collectDailyAgg(template, greenhouseId, sensorType, startZdt, stopZdt, "min", acc, "min");
        collectDailyAgg(template, greenhouseId, sensorType, startZdt, stopZdt, "max", acc, "max");
        collectDailyAgg(template, greenhouseId, sensorType, startZdt, stopZdt, "count", acc, "count");

        Map<LocalDate, DailyStat> result = new TreeMap<>();
        for (Map.Entry<LocalDate, DayAcc> e : acc.entrySet()) {
            DayAcc a = e.getValue();
            if (a.n == 0) continue;
            result.put(e.getKey(), DailyStat.builder()
                    .date(e.getKey())
                    .avg(a.avgSum / a.n)
                    .min(a.min == Double.MAX_VALUE ? null : a.min)
                    .max(a.max == -Double.MAX_VALUE ? null : a.max)
                    .count(a.count)
                    .build());
        }
        return result;
    }

    private void collectDailyAgg(String template, Long greenhouseId, String sensorType,
                                 ZonedDateTime start, ZonedDateTime stop, String fn,
                                 Map<LocalDate, DayAcc> acc, String field) {
        String flux = String.format(template, configHelper.getBucket(), start.toInstant(), stop.toInstant(),
                greenhouseId, sensorType, fn, fn);
        List<FluxTable> tables = queryApi.query(flux);
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Object value = record.getValue();
                Instant time = record.getTime();
                if (value == null || time == null) continue;
                LocalDate date = time.atZone(SENSOR_ZONE).toLocalDate();
                DayAcc a = acc.computeIfAbsent(date, k -> new DayAcc());
                switch (field) {
                    case "avg" -> { a.avgSum += (Double) value; a.n++; }
                    case "min" -> a.min = Math.min(a.min, (Double) value);
                    case "max" -> a.max = Math.max(a.max, (Double) value);
                    case "count" -> a.count += (Long) value;
                    default -> { }
                }
            }
        }
    }

    /** 单日聚合累加器（内部类） */
    private static class DayAcc {
        double avgSum = 0;
        int n = 0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        long count = 0;
    }

    /** 日聚合结果（内部类） */
    @lombok.Getter
    @lombok.Builder
    public static class DailyStat {
        private final LocalDate date;
        private final Double avg;
        private final Double min;
        private final Double max;
        private final Long count;
    }

    // ===== 辅助方法 =====

    private String getDeviceName(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .map(Device::getName)
                .orElse("未知设备");
    }
}
