package com.greenhouse.module.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 实时传感器数据推送消息
 * <p>
 * 推送到 /topic/greenhouse/{greenhouseId}/realtime
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealtimeMessage {

    /** 消息类型：SENSOR_DATA */
    private String type;

    /** 大棚ID */
    private Long greenhouseId;

    /** 设备ID */
    private Long deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 传感器类型 */
    private String sensorType;

    /** 传感器读数 */
    private Double value;

    /** 数据时间 */
    private Instant timestamp;
}
