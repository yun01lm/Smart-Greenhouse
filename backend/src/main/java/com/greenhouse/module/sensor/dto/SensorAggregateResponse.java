package com.greenhouse.module.sensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合统计数据响应
 * <p>
 * 大棚级别：平均/最高/最低/最新值。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorAggregateResponse {

    /** 大棚ID */
    private Long greenhouseId;

    /** 传感器类型 */
    private String sensorType;

    /** 平均值 */
    private Double avgValue;

    /** 最大值 */
    private Double maxValue;

    /** 最小值 */
    private Double minValue;

    /** 最新值 */
    private Double latestValue;

    /** 数据条数 */
    private Long dataCount;
}
