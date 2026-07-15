package com.greenhouse.ai.mock;

import com.greenhouse.module.weather.dto.WeatherCurrentResponse;
import com.greenhouse.module.weather.dto.WeatherForecastResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mock 天气服务
 * <p>
 * 验收时使用，返回模拟的天气数据，不调用和风天气 API。
 * </p>
 */
@Slf4j
@Component("mockWeatherService")
@ConditionalOnProperty(name = "weather.provider", havingValue = "mock", matchIfMissing = true)
public class MockWeatherService {

    public WeatherCurrentResponse getCurrentWeather(String location) {
        log.info("[MOCK] 天气查询: location={}", location);
        return WeatherCurrentResponse.builder()
                .temperature(BigDecimal.valueOf(26.5))
                .feelsLike(BigDecimal.valueOf(28.0))
                .humidity(BigDecimal.valueOf(65))
                .windSpeed(BigDecimal.valueOf(12.0))
                .windDirection("东南风")
                .pressure(BigDecimal.valueOf(1013))
                .visibility(BigDecimal.valueOf(10.0))
                .weatherText("晴")
                .weatherCode("100")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public WeatherForecastResponse getForecast(String location, int days) {
        log.info("[MOCK] 天气预报: location={}, days={}", location, days);
        var today = WeatherForecastResponse.DayForecast.builder()
                .date(LocalDate.of(2026, 7, 15))
                .tempMax(BigDecimal.valueOf(32.0))
                .tempMin(BigDecimal.valueOf(22.0))
                .weatherText("晴转多云").weatherCode("101")
                .humidity(BigDecimal.valueOf(60))
                .windSpeed(BigDecimal.valueOf(10.0))
                .precipitation(BigDecimal.valueOf(0.0))
                .build();
        var tomorrow = WeatherForecastResponse.DayForecast.builder()
                .date(LocalDate.of(2026, 7, 16))
                .tempMax(BigDecimal.valueOf(34.0))
                .tempMin(BigDecimal.valueOf(24.0))
                .weatherText("多云").weatherCode("101")
                .humidity(BigDecimal.valueOf(65))
                .windSpeed(BigDecimal.valueOf(8.0))
                .precipitation(BigDecimal.valueOf(0.0))
                .build();
        return WeatherForecastResponse.builder()
                .location(location)
                .forecasts(List.of(today, tomorrow))
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
