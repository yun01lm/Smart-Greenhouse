package com.greenhouse.module.weather.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.weather.dto.WeatherCurrentResponse;
import com.greenhouse.module.weather.dto.WeatherForecastResponse;
import com.greenhouse.module.weather.service.QWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 天气信息 API
 * <p>
 * 路径前缀：/api/v1/weather
 * </p>
 *
 * <h3>端点列表</h3>
 * <ul>
 *   <li>GET /api/v1/weather/current — 当前天气</li>
 *   <li>GET /api/v1/weather/forecast — 天气预报（3天/7天）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final QWeatherService weatherService;

    /**
     * 当前天气
     * GET /api/v1/weather/current?location=北京
     *
     * @param location 位置标识（城市名或 LocationID）
     */
    @GetMapping("/current")
    public ApiResponse<WeatherCurrentResponse> current(
            @RequestParam(defaultValue = "北京") String location) {

        WeatherCurrentResponse result = weatherService.getCurrentWeather(location);
        return ApiResponse.success(result);
    }

    /**
     * 天气预报
     * GET /api/v1/weather/forecast?location=北京&days=3
     *
     * @param location 位置标识
     * @param days     预报天数（3 或 7，默认 3）
     */
    @GetMapping("/forecast")
    public ApiResponse<WeatherForecastResponse> forecast(
            @RequestParam(defaultValue = "北京") String location,
            @RequestParam(defaultValue = "3") int days) {

        WeatherForecastResponse result = weatherService.getForecast(location, days);
        return ApiResponse.success(result);
    }
}
