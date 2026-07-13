package com.greenhouse.module.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 当前天气响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherCurrentResponse {

    /** 位置名称 */
    private String location;

    /** 当前温度（°C） */
    private BigDecimal temperature;

    /** 体感温度（°C） */
    private BigDecimal feelsLike;

    /** 湿度（%） */
    private BigDecimal humidity;

    /** 天气代码 */
    private String weatherCode;

    /** 天气描述 */
    private String weatherText;

    /** 风速（m/s） */
    private BigDecimal windSpeed;

    /** 风向 */
    private String windDirection;

    /** 气压（hPa） */
    private BigDecimal pressure;

    /** 能见度（km） */
    private BigDecimal visibility;

    /** 数据更新时间 */
    private LocalDateTime updatedAt;
}
