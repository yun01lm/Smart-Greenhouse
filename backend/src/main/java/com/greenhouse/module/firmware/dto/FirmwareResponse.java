package com.greenhouse.module.firmware.dto;

import com.greenhouse.entity.Device;
import com.greenhouse.entity.Firmware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 固件信息响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirmwareResponse {

    private String firmwareId;
    private Device.DeviceType deviceType;
    private Device.SensorType sensorType;
    private String firmwareVersion;
    private String batchNo;
    private Firmware.Status status;
    private Long boundDeviceId;
    private LocalDateTime createdAt;

    public static FirmwareResponse fromEntity(Firmware f) {
        return FirmwareResponse.builder()
                .firmwareId(f.getFirmwareId())
                .deviceType(f.getDeviceType())
                .sensorType(f.getSensorType())
                .firmwareVersion(f.getFirmwareVersion())
                .batchNo(f.getBatchNo())
                .status(f.getStatus())
                .boundDeviceId(f.getBoundDeviceId())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
