package com.greenhouse.module.sensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 环境参数短期预测响应
 * <p>对应 PRD US-005：预测未来 30 分钟 ~ 2 小时的环境参数变化。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForecastResponse {

    private Long greenhouseId;

    private String sensorType;

    /** 预测步长（分钟） */
    private Integer intervalMinutes;

    private List<ForecastPoint> points = new ArrayList<>();
}