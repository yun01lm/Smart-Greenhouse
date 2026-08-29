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

    /**
     * 固件ID（8位数字，出厂预注册）
     * <p>绑定流程必填；创建时系统据此绑定固件并自动生成设备编号(SN)。</p>
     */
    private String firmwareId;

    /**
     * 设备编号
     * <p>绑定流程中由系统自动生成（GH{大棚ID}-{序号}），此处不再由用户填写。</p>
     */
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
