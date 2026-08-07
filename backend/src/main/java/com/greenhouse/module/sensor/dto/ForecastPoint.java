package com.greenhouse.module.sensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 短期预测数据点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForecastPoint {

    /** 预测时间点 */
    private Instant timestamp;

    /** 预测值 */
    private Double value;
}