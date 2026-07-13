package com.greenhouse.module.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 天气预报响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecastResponse {

    /** 位置名称 */
    private String location;

    /** 逐日预报列表 */
    private List<DayForecast> forecasts;

    /** 数据更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 单日预报
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayForecast {
        /** 日期 */
        private LocalDate date;

        /** 最高温度（°C） */
        private BigDecimal tempMax;

        /** 最低温度（°C） */
        private BigDecimal tempMin;

        /** 天气代码 */
        private String weatherCode;

        /** 天气描述 */
        private String weatherText;

        /** 湿度（%） */
        private BigDecimal humidity;

        /** 风速（m/s） */
        private BigDecimal windSpeed;

        /** 降水量（mm） */
        private BigDecimal precipitation;
    }
}
