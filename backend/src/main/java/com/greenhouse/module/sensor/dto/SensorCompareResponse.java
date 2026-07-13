package com.greenhouse.module.sensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 多组传感器对比响应
 * <p>
 * 同一大棚下多个传感器组的同类型数据按时间对齐对比。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorCompareResponse {

    /** 大棚ID */
    private Long greenhouseId;

    /** 传感器类型 */
    private String sensorType;

    /** 参与对比的设备ID列表 */
    private List<Long> deviceIds;

    /** 对比数据：每个设备一组时序数据 */
    private List<DeviceSeries> series;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeviceSeries {
        private Long deviceId;
        private String deviceName;
        private List<SensorDataPoint> dataPoints;
    }
}
