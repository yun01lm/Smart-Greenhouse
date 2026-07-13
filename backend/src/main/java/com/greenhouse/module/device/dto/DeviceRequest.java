package com.greenhouse.module.device.dto;

import com.greenhouse.entity.Device;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备创建/更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRequest {

    @NotBlank(message = "设备名称不能为空")
    private String name;

    @NotBlank(message = "设备编号不能为空")
    private String deviceSn;

    @NotNull(message = "设备类型不能为空")
    private Device.DeviceType deviceType;

    /** 传感器子类型（传感器类设备必填） */
    private Device.SensorType sensorType;

    /** 安装位置 */
    private String installLocation;

    /** 设备描述 */
    private String description;
}
