package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 天气数据缓存实体
 * <p>
 * 对应 DB 第 19 号表 weather_cache。
 * 缓存和风天气 API 拉取的天气数据，每 3 小时更新一次。
 * cache_type 区分当前天气(CURRENT)与预报(FORECAST)，避免同 location 互相覆盖。
 * forecast_json 存储完整预报数据（JSON 格式），供多模态融合分析使用。
 * </p>
 */
@Entity
@Table(name = "weather_cache", indexes = {
        @Index(name = "idx_location_type_updated", columnList = "location, cache_type, updated_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 位置标识（如城市名或经纬度） */
    @Column(name = "location", nullable = false, length = 100)
    private String location;

    /** 缓存类型：CURRENT（当前天气）/ FORECAST（预报） */
    @Column(name = "cache_type", nullable = false, length = 20)
    private String cacheType;

    /** 完整预报数据（JSON） */
    @Column(name = "forecast_json", columnDefinition = "JSON")
    private String forecastJson;

    /** 当前温度（°C） */
    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    /** 体感温度（°C） */
    @Column(name = "feels_like", precision = 5, scale = 2)
    private BigDecimal feelsLike;

    /** 当前湿度（%） */
    @Column(name = "humidity", precision = 5, scale = 2)
    private BigDecimal humidity;

    /** 天气代码（和风天气编码） */
    @Column(name = "weather_code", length = 20)
    private String weatherCode;

    /** 天气描述（如"晴"、"多云"） */
    @Column(name = "weather_text", length = 50)
    private String weatherText;

    /** 风向（如"东风"） */
    @Column(name = "wind_direction", length = 20)
    private String windDirection;

    /** 风速（m/s） */
    @Column(name = "wind_speed", precision = 5, scale = 2)
    private BigDecimal windSpeed;

    /** 气压（hPa） */
    @Column(name = "pressure", precision = 7, scale = 1)
    private BigDecimal pressure;

    /** 能见度（km） */
    @Column(name = "visibility", precision = 6, scale = 1)
    private BigDecimal visibility;

    /** 预报时间 */
    @Column(name = "forecast_time")
    private LocalDateTime forecastTime;

    /** 更新时间 */
    @Column(name = "updated_at", nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}