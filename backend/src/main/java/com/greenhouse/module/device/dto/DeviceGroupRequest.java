package com.greenhouse.module.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 设备分组创建/更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceGroupRequest {

    @NotBlank(message = "分组名称不能为空")
    private String name;

    /** 分组描述 */
    private String description;

    /** 组内设备ID列表 */
    @Size(max = 50, message = "单个分组最多包含50个设备")
    private List<Long> deviceIds;
}
