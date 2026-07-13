package com.greenhouse.module.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 设备状态变更推送消息
 * <p>
 * 推送到 /topic/device/{deviceId}/status
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceStatusMessage {

    /** 消息类型：DEVICE_STATUS */
    private String type;

    /** 设备ID */
    private Long deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 所属大棚ID */
    private Long greenhouseId;

    /** 设备状态：ONLINE / OFFLINE / ALARM */
    private String status;

    /** 上次数值 */
    private String lastValue;

    /** 状态变更时间 */
    private Instant timestamp;
}
