package com.greenhouse.module.sensor.service;

import com.greenhouse.entity.Device;
import com.greenhouse.repository.DeviceRepository;
import com.greenhouse.module.sensor.dto.*;
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
    private final DeviceRepository deviceRepository;
    private final InfluxDbConfigHelper configHelper;

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
     * 多组传感器数据对比
     */
    public SensorCompareResponse getCompareData(Long greenhouseId, String sensorType,
                                                  List<Long> deviceIds,
                                                  Long startTime, Long endTime) {
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

    // ===== 辅助方法 =====

    private String getDeviceName(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .map(Device::getName)
                .orElse("未知设备");
    }
}
