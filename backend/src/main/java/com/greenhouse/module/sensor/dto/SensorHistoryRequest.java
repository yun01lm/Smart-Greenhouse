package com.greenhouse.module.sensor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 历史数据查询请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorHistoryRequest {

    /** 传感器类型 */
    @NotBlank(message = "传感器类型不能为空")
    private String sensorType;

    /** 开始时间（epoch 毫秒） */
    @NotNull(message = "开始时间不能为空")
    private Long startTime;

    /** 结束时间（epoch 毫秒） */
    @NotNull(message = "结束时间不能为空")
    private Long endTime;

    /** 聚合间隔：1m/5m/15m/1h/6h/1d */
    @Builder.Default
    private String interval = "5m";
}
