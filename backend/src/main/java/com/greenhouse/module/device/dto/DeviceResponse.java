package com.greenhouse.module.device.dto;

import com.greenhouse.entity.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备信息响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {

    private Long id;
    private String name;
    private String deviceSn;
    private String firmwareId;
    private Device.DeviceType deviceType;
    private Device.SensorType sensorType;
    private Device.DeviceStatus status;
    private Long greenhouseId;
    private String lastValue;
    private LocalDateTime lastDataTime;
    private String mqttTopic;
    private String installLocation;
    private String description;
    private LocalDateTime createdAt;

    public static DeviceResponse fromEntity(Device d) {
        return DeviceResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .deviceSn(d.getDeviceSn())
                .firmwareId(d.getFirmwareId())
                .deviceType(d.getDeviceType())
                .sensorType(d.getSensorType())
                .status(d.getStatus())
                .greenhouseId(d.getGreenhouseId())
                .lastValue(d.getLastValue())
                .lastDataTime(d.getLastDataTime())
                .mqttTopic(d.getMqttTopic())
                .installLocation(d.getInstallLocation())
                .description(d.getDescription())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
