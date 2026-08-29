package com.greenhouse.module.firmware.dto;

import com.greenhouse.entity.Device;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 固件批量预注册请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirmwareBatchRequest {

    /** 预注册数量（1~1000） */
    @NotNull(message = "预注册数量不能为空")
    @Min(value = 1, message = "预注册数量至少为1")
    @Max(value = 1000, message = "单次预注册数量不能超过1000")
    private Integer count;

    /** 设备类型（传感器/控制器） */
    @NotNull(message = "设备类型不能为空")
    private Device.DeviceType deviceType;

    /** 传感器子类型（传感器类固件必填） */
    private Device.SensorType sensorType;

    /** 固件版本号，如 "1.0.0" */
    private String firmwareVersion;

    /** 出厂批次号，如 "B20260801" */
    private String batchNo;
}
