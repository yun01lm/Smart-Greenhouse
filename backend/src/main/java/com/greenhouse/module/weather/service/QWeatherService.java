package com.greenhouse.module.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.WeatherCache;
import com.greenhouse.module.weather.dto.WeatherCurrentResponse;
import com.greenhouse.module.weather.dto.WeatherForecastResponse;
import com.greenhouse.repository.WeatherCacheRepository;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 和风天气 API 服务
 * <p>
 * 提供当前天气和天气预报查询，数据缓存到 weather_cache 表。
 * 缓存策略：3 小时内直接返回缓存，过期则调用 API 刷新。
 * </p>
 *
 * <h3>API 端点</h3>
 * <ul>
 *   <li>当前天气：GET https://devapi.qweather.com/v7/weather/now?location=xxx&key=xxx</li>
 *   <li>3天预报：GET https://devapi.qweather.com/v7/weather/3d?location=xxx&key=xxx</li>
 * </ul>
 */
@Slf4j
@Service
public class QWeatherService {

    private final String apiKey;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final WeatherCacheRepository cacheRepository;

    /** 缓存有效期：3 小时 */
    private static final long CACHE_DURATION_HOURS = 3;

    /** 缓存类型：当前天气 */
    private static final String CACHE_TYPE_CURRENT = "CURRENT";

    /** 缓存类型：预报 */
    private static final String CACHE_TYPE_FORECAST = "FORECAST";

    /** 中文地名 → 和风 LocationID 内存缓存（避免重复调用 GeoAPI） */
    private final Map<String, String> locationIdCache = new ConcurrentHashMap<>();

    /** 天气数据提供者：mock（本地模拟）或 qweather（和风天气 API） */
    @Value("${weather.provider:mock}")
    private String provider;

    public QWeatherService(
            @Value("${qweather.api-key}") String apiKey,
            @Value("${qweather.base-url}") String baseUrl,
            ObjectMapper objectMapper,
            WeatherCacheRepository cacheRepository) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
        this.cacheRepository = cacheRepository;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 获取当前天气
     *
     * @param location 位置标识（城市名或 LocationID）
     */
    public WeatherCurrentResponse getCurrentWeather(String location) {
        // Mock 模式：直接返回模拟数据
        if ("mock".equals(provider)) {
            return buildMockCurrent(location);
        }

        // 检查缓存
        Optional<WeatherCache> cached = cacheRepository.findTopByLocationAndCacheTypeOrderByUpdatedAtDesc(location, CACHE_TYPE_CURRENT);
        if (cached.isPresent()) {
            WeatherCache cache = cached.get();
            if (isCacheValid(cache)) {
                log.debug("天气缓存命中: location={}", location);
                return buildCurrentFromCache(cache);
            }
        }

        // 缓存过期或不存在，调用 API
        return fetchCurrentWeather(location);
    }

    /**
     * 获取天气预报
     *
     * @param location 位置标识
     * @param days     预报天数（3 或 7）
     */
    public WeatherForecastResponse getForecast(String location, int days) {
        // Mock 模式：直接返回模拟数据
        if ("mock".equals(provider)) {
            return buildMockForecast(location, days);
        }

        // 检查缓存
        Optional<WeatherCache> cached = cacheRepository.findTopByLocationAndCacheTypeOrderByUpdatedAtDesc(location, CACHE_TYPE_FORECAST);
        if (cached.isPresent()) {
            WeatherCache cache = cached.get();
            if (isCacheValid(cache) && cache.getForecastJson() != null) {
                log.debug("预报缓存命中: location={}", location);
                return parseForecastFromJson(cache.getForecastJson(), location);
            }
        }

        return fetchForecast(location, days);
    }

    // ===== 私有方法 =====

    /**
     * 判断缓存是否有效（3 小时内）
     */
    private boolean isCacheValid(WeatherCache cache) {
        long hoursSinceUpdate = Duration.between(cache.getUpdatedAt(), LocalDateTime.now()).toHours();
        return hoursSinceUpdate < CACHE_DURATION_HOURS;
    }

    /**
     * 从缓存构建当前天气响应
     */
    private WeatherCurrentResponse buildCurrentFromCache(WeatherCache cache) {
        return WeatherCurrentResponse.builder()
                .location(cache.getLocation())
                .temperature(cache.getTemperature())
                .feelsLike(cache.getFeelsLike())
                .humidity(cache.getHumidity())
                .weatherCode(cache.getWeatherCode())
                .weatherText(cache.getWeatherText())
                .windDirection(cache.getWindDirection())
                .windSpeed(cache.getWindSpeed())
                .pressure(cache.getPressure())
                .visibility(cache.getVisibility())
                .updatedAt(cache.getUpdatedAt())
                .build();
    }

    /**
     * 解析 API 可用 location（LocationID / 经纬度 / 中文地名→GeoAPI 查询）
     * <p>
     * 和风 v7 天气 API 的 location 参数仅支持 LocationID、经纬度或拼音，不支持中文地名。
     * 这里通过 GeoAPI（/geo/v2/city/lookup）将中文地名转换为 LocationID，结果内存缓存；
     * 已为 LocationID（纯数字）或经纬度（含逗号）时直接使用。
     * 转换失败时降级返回原值（交由 API 错误处理兜底）。
     * </p>
     */
    private String resolveApiLocation(String location) {
        if (location == null || location.isBlank()) {
            return location;
        }
        String trimmed = location.trim();
        // 已是 LocationID（纯数字）或经纬度（含逗号）→ 直接使用
        if (trimmed.matches("\\d+") || trimmed.contains(",")) {
            return trimmed;
        }
        // 内存缓存命中
        String cached = locationIdCache.get(trimmed);
        if (cached != null) {
            return cached;
        }
        try {
            String id = lookupLocationId(trimmed);
            if (id != null && !id.isBlank()) {
                locationIdCache.put(trimmed, id);
                log.info("中文地名已转换: {} → LocationID={}", trimmed, id);
                return id;
            }
        } catch (Exception e) {
            log.warn("地理编码失败，降级使用原 location: {}, error={}", trimmed, e.getMessage());
        }
        return trimmed;
    }

    /**
     * 调用和风 GeoAPI 查询中文地名的 LocationID
     * GET {baseUrl}/geo/v2/city/lookup?location=北京&number=1&key=xxx
     */
    private String lookupLocationId(String name) throws Exception {
        String url = baseUrl + "/geo/v2/city/lookup?location="
                + URLEncoder.encode(name, "UTF-8") + "&number=1&key=" + apiKey;
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("GeoAPI 请求失败: HTTP {} location={}", response.code(), name);
                return null;
            }
            String body = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(body);
            if (!"200".equals(root.get("code").asText())) {
                log.warn("GeoAPI 返回错误: code={} location={}", root.get("code").asText(), name);
                return null;
            }
            JsonNode locationArray = root.get("location");
            if (locationArray == null || !locationArray.isArray() || locationArray.isEmpty()) {
                log.warn("GeoAPI 未找到匹配: location={}", name);
                return null;
            }
            JsonNode first = locationArray.get(0);
            return first.has("id") ? first.get("id").asText() : null;
        }
    }

    /**
     * 调用和风天气 API 获取当前天气
     */
    private WeatherCurrentResponse fetchCurrentWeather(String location) {
        try {
            String url = baseUrl + "/v7/weather/now?location=" + resolveApiLocation(location) + "&key=" + apiKey;

            Request request = new Request.Builder().url(url).get().build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("和风天气 API 请求失败: HTTP {} location={}", response.code(), location);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }

                String body = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(body);

                // 检查 API 返回码
                String code = root.get("code").asText();
                if (!"200".equals(code)) {
                    log.error("和风天气 API 错误: code={} location={}", code, location);
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }

                JsonNode now = root.get("now");
                BigDecimal temp = new BigDecimal(now.get("temp").asText());
                BigDecimal feelsLike = new BigDecimal(now.get("feelsLike").asText());
                BigDecimal humidity = new BigDecimal(now.get("humidity").asText());
                String weatherCode = now.get("icon").asText();
                String weatherText = now.get("text").asText();
                BigDecimal windSpeed = new BigDecimal(now.get("windSpeed").asText());
                String windDir = now.get("windDir").asText();
                BigDecimal pressure = new BigDecimal(now.get("pressure").asText());
                BigDecimal visibility = new BigDecimal(now.get("vis").asText());

                // 保存缓存
                saveWeatherCache(location, CACHE_TYPE_CURRENT, temp, feelsLike, humidity,
                        weatherCode, weatherText, windDir, windSpeed, pressure, visibility, null);

                return WeatherCurrentResponse.builder()
                        .location(location)
                        .temperature(temp)
                        .feelsLike(feelsLike)
                        .humidity(humidity)
                        .weatherCode(weatherCode)
                        .weatherText(weatherText)
                        .windSpeed(windSpeed)
                        .windDirection(windDir)
                        .pressure(pressure)
                        .visibility(visibility)
                        .updatedAt(LocalDateTime.now())
                        .build();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取当前天气异常: location={} error={}", location, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 调用和风天气 API 获取天气预报
     */
    private WeatherForecastResponse fetchForecast(String location, int days) {
        try {
            String apiPath = days <= 3 ? "/v7/weather/3d" : "/v7/weather/7d";
            String url = baseUrl + apiPath + "?location=" + resolveApiLocation(location) + "&key=" + apiKey;

            Request request = new Request.Builder().url(url).get().build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }

                String body = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(body);

                String code = root.get("code").asText();
                if (!"200".equals(code)) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR);
                }

                // 解析逐日预报
                JsonNode dailyArray = root.get("daily");
                List<WeatherForecastResponse.DayForecast> forecasts = new ArrayList<>();

                for (JsonNode day : dailyArray) {
                    forecasts.add(WeatherForecastResponse.DayForecast.builder()
                            .date(LocalDate.parse(day.get("fxDate").asText()))
                            .tempMax(new BigDecimal(day.get("tempMax").asText()))
                            .tempMin(new BigDecimal(day.get("tempMin").asText()))
                            .weatherCode(day.get("iconDay").asText())
                            .weatherText(day.get("textDay").asText())
                            .humidity(new BigDecimal(day.get("humidity").asText()))
                            .windSpeed(new BigDecimal(day.get("windSpeedDay").asText()))
                            .precipitation(new BigDecimal(day.get("precip").asText()))
                            .build());
                }

                // 保存缓存（完整 JSON + 当前天气摘要）
                String forecastJson = objectMapper.writeValueAsString(root);
                JsonNode firstDay = dailyArray.get(0);
                saveWeatherCache(location, CACHE_TYPE_FORECAST,
                        new BigDecimal(firstDay.get("tempMax").asText()),  // 用最高温近似
                        null,
                        new BigDecimal(firstDay.get("humidity").asText()),
                        firstDay.get("iconDay").asText(),
                        firstDay.get("textDay").asText(),
                        firstDay.get("windDirDay").asText(),
                        new BigDecimal(firstDay.get("windSpeedDay").asText()),
                        null, null,
                        forecastJson);

                return WeatherForecastResponse.builder()
                        .location(location)
                        .forecasts(forecasts)
                        .updatedAt(LocalDateTime.now())
                        .build();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取天气预报异常: location={} error={}", location, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 从缓存 JSON 解析预报数据
     */
    private WeatherForecastResponse parseForecastFromJson(String forecastJson, String location) {
        try {
            JsonNode root = objectMapper.readTree(forecastJson);
            JsonNode dailyArray = root.get("daily");
            List<WeatherForecastResponse.DayForecast> forecasts = new ArrayList<>();

            if (dailyArray != null) {
                for (JsonNode day : dailyArray) {
                    forecasts.add(WeatherForecastResponse.DayForecast.builder()
                            .date(LocalDate.parse(day.get("fxDate").asText()))
                            .tempMax(new BigDecimal(day.get("tempMax").asText()))
                            .tempMin(new BigDecimal(day.get("tempMin").asText()))
                            .weatherCode(day.get("iconDay").asText())
                            .weatherText(day.get("textDay").asText())
                            .humidity(new BigDecimal(day.get("humidity").asText()))
                            .windSpeed(new BigDecimal(day.get("windSpeedDay").asText()))
                            .precipitation(new BigDecimal(day.get("precip").asText()))
                            .build());
                }
            }

            return WeatherForecastResponse.builder()
                    .location(location)
                    .forecasts(forecasts)
                    .updatedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("解析预报缓存失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 保存天气数据到缓存
     */
    @Transactional
    private void saveWeatherCache(String location, String cacheType,
                                   BigDecimal temperature, BigDecimal feelsLike,
                                   BigDecimal humidity, String weatherCode, String weatherText,
                                   String windDirection, BigDecimal windSpeed,
                                   BigDecimal pressure, BigDecimal visibility,
                                   String forecastJson) {
        WeatherCache cache = WeatherCache.builder()
                .location(location)
                .cacheType(cacheType)
                .temperature(temperature)
                .feelsLike(feelsLike)
                .humidity(humidity)
                .weatherCode(weatherCode)
                .weatherText(weatherText)
                .windDirection(windDirection)
                .windSpeed(windSpeed)
                .pressure(pressure)
                .visibility(visibility)
                .forecastJson(forecastJson)
                .forecastTime(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        cacheRepository.save(cache);
        log.debug("天气缓存已更新: location={} type={} temp={}", location, cacheType, temperature);
    }

    /**
     * 生成模拟当前天气数据
     */
    private WeatherCurrentResponse buildMockCurrent(String location) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        BigDecimal temp = BigDecimal.valueOf(r.nextDouble(15.0, 35.0)).setScale(1, java.math.RoundingMode.HALF_UP);
        BigDecimal feelsLike = temp.add(BigDecimal.valueOf(r.nextDouble(-2.0, 2.0))).setScale(1, java.math.RoundingMode.HALF_UP);
        BigDecimal humidity = BigDecimal.valueOf(r.nextDouble(40.0, 90.0)).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal windSpeed = BigDecimal.valueOf(r.nextDouble(0.0, 15.0)).setScale(1, java.math.RoundingMode.HALF_UP);
        BigDecimal pressure = BigDecimal.valueOf(r.nextDouble(1000.0, 1020.0)).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal visibility = BigDecimal.valueOf(r.nextDouble(5.0, 30.0)).setScale(0, java.math.RoundingMode.HALF_UP);

        String[] weatherCodes = {"100", "101", "102", "103", "104", "300", "301", "302", "305"};
        String[] weatherTexts = {"晴", "多云", "少云", "晴间多云", "多云", "阵雨", "强阵雨", "雷阵雨", "小雨"};
        String[] windDirs = {"北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风"};
        int idx = r.nextInt(weatherCodes.length);

        return WeatherCurrentResponse.builder()
                .location(location)
                .temperature(temp)
                .feelsLike(feelsLike)
                .humidity(humidity)
                .weatherCode(weatherCodes[idx])
                .weatherText(weatherTexts[idx])
                .windSpeed(windSpeed)
                .windDirection(windDirs[r.nextInt(windDirs.length)])
                .pressure(pressure)
                .visibility(visibility)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 生成模拟天气预报数据
     */
    private WeatherForecastResponse buildMockForecast(String location, int days) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int actualDays = Math.min(days, 7);
        List<WeatherForecastResponse.DayForecast> forecasts = new ArrayList<>();

        String[] weatherCodes = {"100", "101", "102", "104", "300", "301", "305"};
        String[] weatherTexts = {"晴", "多云", "少云", "多云", "阵雨", "强阵雨", "小雨"};

        for (int i = 0; i < actualDays; i++) {
            double baseTemp = r.nextDouble(20.0, 32.0);
            int idx = r.nextInt(weatherCodes.length);
            forecasts.add(WeatherForecastResponse.DayForecast.builder()
                    .date(LocalDate.now().plusDays(i))
                    .tempMax(BigDecimal.valueOf(baseTemp + r.nextDouble(0.0, 3.0)).setScale(1, java.math.RoundingMode.HALF_UP))
                    .tempMin(BigDecimal.valueOf(baseTemp - r.nextDouble(5.0, 10.0)).setScale(1, java.math.RoundingMode.HALF_UP))
                    .weatherCode(weatherCodes[idx])
                    .weatherText(weatherTexts[idx])
                    .humidity(BigDecimal.valueOf(r.nextDouble(40.0, 90.0)).setScale(0, java.math.RoundingMode.HALF_UP))
                    .windSpeed(BigDecimal.valueOf(r.nextDouble(0.0, 15.0)).setScale(1, java.math.RoundingMode.HALF_UP))
                    .precipitation(BigDecimal.valueOf(r.nextDouble(0.0, 10.0)).setScale(1, java.math.RoundingMode.HALF_UP))
                    .build());
        }

        return WeatherForecastResponse.builder()
                .location(location)
                .forecasts(forecasts)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
