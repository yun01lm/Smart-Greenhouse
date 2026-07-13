package com.greenhouse.module.control.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备控制请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlRequest {

    /** 设备ID */
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    /** 控制动作：ON / OFF */
    @NotBlank(message = "控制动作不能为空")
    private String action;
}
