package com.greenhouse.module.sensor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 实时数据响应
 * <p>
 * 返回大棚下各传感器类型的最新读数。
 * key = 传感器类型（如 TEMPERATURE），value = 最新数据点列表（多组传感器）。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorRealtimeResponse {

    /** 大棚ID */
    private Long greenhouseId;

    /** 大棚名称 */
    private String greenhouseName;

    /** 按传感器类型分组的最新数据 */
    private Map<String, List<SensorDataPoint>> dataByType;
}
