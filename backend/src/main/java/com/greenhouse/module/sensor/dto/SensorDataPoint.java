package com.greenhouse.module.sensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 传感器数据点（通用）
 * <p>
 * 用于 InfluxDB 读写和 API 响应。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDataPoint {

    /** 大棚ID */
    private Long greenhouseId;

    /** 设备ID */
    private Long deviceId;

    /** 传感器类型 */
    private String sensorType;

    /** 传感器读数 */
    private Double value;

    /** 数据时间戳 */
    private Instant timestamp;

    /** 设备名称（查询时填充） */
    private String deviceName;
}
