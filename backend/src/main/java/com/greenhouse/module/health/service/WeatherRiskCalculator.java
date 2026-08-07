package com.greenhouse.module.health.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.entity.WeatherCache;
import com.greenhouse.repository.WeatherCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 天气风险修正因子计算器
 * <p>
 * 基于天气缓存数据计算天气对作物健康的影响因子。
 * 修正逻辑：
 * <ul>
 *   <li>极端天气（暴雨/暴雪/大风/冰雹/极端温度）→ factor = 0.7</li>
 *   <li>连续3天高温 (>35°C) → factor = 0.75</li>
 *   <li>小雨/阴天/降温 → factor = 0.9</li>
 *   <li>晴天/适宜天气 → factor = 1.0</li>
 *   <li>无天气数据 → factor = 1.0 (无影响)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherRiskCalculator {

    private final WeatherCacheRepository weatherCacheRepository;
    private final ObjectMapper objectMapper;

    /** 极端天气代码（和风天气编码） */
    private static final String[] EXTREME_CODES = {
            "302", // 雷阵雨
            "303", // 强雷阵雨
            "304", // 雷阵雨伴有冰雹
            "307", // 大雨
            "310", // 暴雨
            "311", // 大暴雨
            "312", // 特大暴雨
            "313", // 冻雨
            "403", // 暴雪
            "404", // 雨夹雪
            "405", // 雨雪天气
            "406", // 阵雨夹雪
            "407", // 阵雪
            "503", // 浮尘
            "504", // 扬沙
            "507", // 沙尘暴
            "508"  // 强沙尘暴
    };

    /** 轻微不利天气代码 */
    private static final String[] MILD_CODES = {
            "101", // 多云
            "102", // 少云
            "103", // 晴间多云
            "104", // 阴
            "305", // 小雨
            "306", // 中雨
            "309", // 毛毛雨/细雨
            "402", // 小雪
            "500", // 薄雾
            "501", // 雾
            "502"  // 霾
    };

    /**
     * 计算天气修正因子并返回风险描述
     *
     * @param location 位置标识（城市名或经纬度）
     * @return WeatherRiskResult 包含修正因子和风险描述
     */
    public WeatherRiskResult calculate(String location) {
        // 天气风险基于当前天气缓存（CURRENT 类型）
        Optional<WeatherCache> cacheOpt = weatherCacheRepository
                .findTopByLocationAndCacheTypeOrderByUpdatedAtDesc(location, "CURRENT");

        if (cacheOpt.isEmpty()) {
            log.debug("无天气缓存数据，天气修正因子使用默认值 1.0");
            return new WeatherRiskResult(1.0, "无天气数据");
        }

        WeatherCache cache = cacheOpt.get();
        double factor = 1.0;
        String riskDescription = "适宜天气";
        StringBuilder riskDetails = new StringBuilder();

        // 1. 检查极端温度
        BigDecimal temp = cache.getTemperature();
        if (temp != null) {
            double t = temp.doubleValue();
            if (t > 40.0) {
                factor = Math.min(factor, 0.7);
                riskDetails.append("极端高温(").append(t).append("°C)");
                riskDescription = "极端高温风险";
            } else if (t < 0.0) {
                factor = Math.min(factor, 0.7);
                riskDetails.append("寒潮(").append(t).append("°C)");
                riskDescription = "寒潮风险";
            } else if (t > 35.0) {
                factor = Math.min(factor, 0.85);
                riskDetails.append("高温(").append(t).append("°C)");
                riskDescription = "高温风险";
            } else if (t < 5.0) {
                factor = Math.min(factor, 0.85);
                riskDetails.append("低温(").append(t).append("°C)");
                riskDescription = "低温风险";
            }
        }

        // 2. 检查极端天气代码
        String weatherCode = cache.getWeatherCode();
        if (weatherCode != null) {
            if (isExtremeWeather(weatherCode)) {
                factor = Math.min(factor, 0.7);
                if (!riskDetails.isEmpty()) riskDetails.append("; ");
                riskDetails.append("极端天气(编码").append(weatherCode).append(")");
                riskDescription = "极端天气风险";
            } else if (isMildWeather(weatherCode)) {
                factor = Math.min(factor, 0.9);
                if (riskDescription.equals("适宜天气")) {
                    riskDescription = "轻微不利天气";
                }
            }
        }

        // 3. 检查大风
        BigDecimal windSpeed = cache.getWindSpeed();
        if (windSpeed != null) {
            double ws = windSpeed.doubleValue();
            if (ws > 20.0) {
                factor = Math.min(factor, 0.7);
                if (!riskDetails.isEmpty()) riskDetails.append("; ");
                riskDetails.append("大风(").append(ws).append("m/s)");
                riskDescription = "大风风险";
            } else if (ws > 10.0) {
                factor = Math.min(factor, 0.9);
            }
        }

        // 4. 检查连续高温（解析 forecast_json）
        if (cache.getForecastJson() != null) {
            try {
                JsonNode forecast = objectMapper.readTree(cache.getForecastJson());
                if (forecast.has("daily")) {
                    int consecutiveHotDays = countConsecutiveHotDays(forecast.get("daily"), 35.0);
                    if (consecutiveHotDays >= 3) {
                        factor = Math.min(factor, 0.75);
                        riskDescription = "连续高温预警";
                        if (!riskDetails.isEmpty()) riskDetails.append("; ");
                        riskDetails.append("连续").append(consecutiveHotDays).append("天高温");
                    }
                }
            } catch (Exception e) {
                log.warn("解析天气预报JSON失败: {}", e.getMessage());
            }
        }

        log.debug("天气修正: location={}, factor={:.2f}, risk={}", location, factor, riskDescription);
        return new WeatherRiskResult(factor, riskDescription);
    }

    /**
     * 判断是否为极端天气代码
     */
    private boolean isExtremeWeather(String code) {
        for (String extreme : EXTREME_CODES) {
            if (extreme.equals(code)) return true;
        }
        return false;
    }

    /**
     * 判断是否为轻微不利天气代码
     */
    private boolean isMildWeather(String code) {
        for (String mild : MILD_CODES) {
            if (mild.equals(code)) return true;
        }
        return false;
    }

    /**
     * 统计连续高温天数
     */
    private int countConsecutiveHotDays(JsonNode daily, double threshold) {
        int maxConsecutive = 0;
        int currentStreak = 0;

        for (JsonNode day : daily) {
            JsonNode tempMax = day.get("tempMax");
            if (tempMax != null && tempMax.asDouble() > threshold) {
                currentStreak++;
                maxConsecutive = Math.max(maxConsecutive, currentStreak);
            } else {
                currentStreak = 0;
            }
        }

        return maxConsecutive;
    }

    /**
     * 天气风险计算结果
     */
    public record WeatherRiskResult(double factor, String description) {}
}
