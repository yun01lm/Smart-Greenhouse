package com.greenhouse.module.sensor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 多组传感器对比请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompareRequest {

    @NotNull(message = "大棚ID不能为空")
    private Long greenhouseId;

    @NotBlank(message = "传感器类型不能为空")
    private String sensorType;

    @NotEmpty(message = "设备ID列表不能为空")
    private List<Long> deviceIds;

    @NotNull(message = "开始时间不能为空")
    private Long startTime;

    @NotNull(message = "结束时间不能为空")
    private Long endTime;
}
