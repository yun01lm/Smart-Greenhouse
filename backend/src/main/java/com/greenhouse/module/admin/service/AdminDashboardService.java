package com.greenhouse.module.admin.service;

import com.greenhouse.entity.Alert;
import com.greenhouse.entity.Device;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.module.greenhouse.service.RegionService;
import com.greenhouse.module.sensor.dto.SensorDataPoint;
import com.greenhouse.module.sensor.dto.SensorRealtimeResponse;
import com.greenhouse.module.sensor.service.SensorDataService;
import com.greenhouse.module.weather.dto.WeatherCurrentResponse;
import com.greenhouse.module.weather.service.QWeatherService;
import com.greenhouse.repository.AlertRepository;
import com.greenhouse.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员数据总览服务（R3）
 * <p>
 * 按地区范围聚合：整体统计、环境平均值、预警总览、地区健康评分、最新预警、当前天气，
 * 并合并原系统监控（AdminMonitorService）的全局运行状态。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final RegionService regionService;
    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final SensorDataService sensorDataService;
    private final QWeatherService weatherService;
    private final AdminMonitorService monitorService;

    /** 传感器类型 → 展示名 */
    private static final Map<String, String> ENV_TYPES = Map.of(
            "TEMPERATURE", "温度",
            "HUMIDITY", "湿度",
            "LIGHT", "光照",
            "CO2", "CO2");

    /**
     * 地区数据总览
     */
    public Map<String, Object> getOverview(String province, String city, String district,
                                           String town, String village) {
        List<Greenhouse> ghs = regionService.getGreenhousesByRegion(province, city, district, town, village);
        List<Long> ghIds = ghs.stream().map(Greenhouse::getId).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("region", Map.of(
                "province", nz(province), "city", nz(city),
                "district", nz(district), "town", nz(town), "village", nz(village)));
        result.put("stats", buildStats(ghs, ghIds));
        result.put("env", buildEnv(ghs));
        result.put("alerts", buildAlertOverview(ghIds));
        result.put("healthScore", buildHealthScore(ghIds));
        result.put("latestAlerts", buildLatestAlerts(ghIds, ghs));
        result.put("weather", buildWeather(province, city, district, town, village));
        result.put("monitor", monitorService.getOverview());
        return result;
    }

    // ===== 1. 整体统计 =====

    private Map<String, Object> buildStats(List<Greenhouse> ghs, List<Long> ghIds) {
        long greenhouseCount = ghs.size();
        long ownerCount = ghs.stream().map(Greenhouse::getOwnerId).distinct().count();

        long deviceTotal = 0;
        long deviceOnline = 0;
        long deviceOffline = 0;
        long ownerOnline = 0;

        if (!ghIds.isEmpty()) {
            List<Device> devices = deviceRepository.findByGreenhouseIdIn(ghIds);
            deviceTotal = devices.size();
            Map<Long, Long> ghOwner = ghs.stream()
                    .collect(Collectors.toMap(Greenhouse::getId, Greenhouse::getOwnerId, (a, b) -> a));
            Map<Long, Boolean> ownerHasOnline = new LinkedHashMap<>();
            for (Device d : devices) {
                boolean isOnline = d.getStatus() == Device.DeviceStatus.ONLINE
                        || d.getStatus() == Device.DeviceStatus.ALARM;
                if (isOnline) {
                    deviceOnline++;
                    Long ownerId = ghOwner.get(d.getGreenhouseId());
                    if (ownerId != null) {
                        ownerHasOnline.put(ownerId, true);
                    }
                } else {
                    deviceOffline++;
                }
            }
            ownerOnline = ownerHasOnline.size();
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("greenhouseCount", greenhouseCount);
        stats.put("ownerCount", ownerCount);
        stats.put("ownerOnline", ownerOnline);
        stats.put("deviceTotal", deviceTotal);
        stats.put("deviceOnline", deviceOnline);
        stats.put("deviceOffline", deviceOffline);
        return stats;
    }

    // ===== 2. 环境聚合（各大棚最新值求平均） =====

    private Map<String, Object> buildEnv(List<Greenhouse> ghs) {
        Map<String, List<Double>> valuesByType = new LinkedHashMap<>();
        int sampled = 0;
        for (Greenhouse gh : ghs) {
            try {
                SensorRealtimeResponse realtime = sensorDataService.getRealtimeData(gh.getId(), gh.getName());
                Map<String, List<SensorDataPoint>> dataByType = realtime.getDataByType();
                for (Map.Entry<String, String> e : ENV_TYPES.entrySet()) {
                    List<SensorDataPoint> points = dataByType.get(e.getKey());
                    if (points != null && !points.isEmpty()) {
                        valuesByType.computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                                .add(points.get(0).getValue());
                    }
                }
                sampled++;
            } catch (Exception ex) {
                log.debug("地区环境聚合跳过大棚 {}: {}", gh.getId(), ex.getMessage());
            }
        }

        Map<String, Object> env = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : ENV_TYPES.entrySet()) {
            List<Double> values = valuesByType.get(e.getKey());
            if (values != null && !values.isEmpty()) {
                double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                env.put(e.getKey(), Map.of("label", e.getValue(), "avg", round1(avg), "count", values.size()));
            } else {
                env.put(e.getKey(), Map.of("label", e.getValue(), "avg", null, "count", 0));
            }
        }
        env.put("sampledGreenhouseCount", sampled);
        return env;
    }

    // ===== 3. 预警总览 =====

    private Map<String, Object> buildAlertOverview(List<Long> ghIds) {
        Map<String, Object> alerts = new LinkedHashMap<>();
        if (ghIds.isEmpty()) {
            alerts.put("total", 0L);
            alerts.put("critical", 0L);
            alerts.put("warning", 0L);
            alerts.put("info", 0L);
            return alerts;
        }
        long total = alertRepository.countByGreenhouseIdIn(ghIds);
        long critical = alertRepository.countByGreenhouseIdInAndLevel(ghIds, Alert.AlertLevel.CRITICAL);
        long warning = alertRepository.countByGreenhouseIdInAndLevel(ghIds, Alert.AlertLevel.WARNING);
        long info = alertRepository.countByGreenhouseIdInAndLevel(ghIds, Alert.AlertLevel.INFO);
        alerts.put("total", total);
        alerts.put("critical", critical);
        alerts.put("warning", warning);
        alerts.put("info", info);
        return alerts;
    }

    // ===== 4. 地区健康评分（按预警加权扣分，规则后续可细化） =====

    private Map<String, Object> buildHealthScore(List<Long> ghIds) {
        Map<String, Object> alerts = buildAlertOverview(ghIds);
        long critical = (long) alerts.get("critical");
        long warning = (long) alerts.get("warning");
        long info = (long) alerts.get("info");
        long total = (long) alerts.get("total");

        int score = 100;
        score -= (int) critical * 8;
        score -= (int) warning * 3;
        score -= (int) info * 1;
        if (score < 0) {
            score = 0;
        }
        String level;
        if (score >= 90) {
            level = "优";
        } else if (score >= 75) {
            level = "良";
        } else if (score >= 60) {
            level = "中";
        } else {
            level = "差";
        }

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("score", score);
        health.put("level", level);
        health.put("alertTotal", total);
        health.put("critical", critical);
        health.put("warning", warning);
        health.put("info", info);
        return health;
    }

    // ===== 5. 最新预警（地区范围，最多 8 条） =====

    private List<Map<String, Object>> buildLatestAlerts(List<Long> ghIds, List<Greenhouse> ghs) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (ghIds.isEmpty()) {
            return result;
        }
        List<Alert> alerts = alertRepository
                .findByGreenhouseIdInOrderByCreatedAtDesc(ghIds, PageRequest.of(0, 8));
        Map<Long, String> ghName = ghs.stream().collect(Collectors.toMap(Greenhouse::getId, Greenhouse::getName, (a, b) -> a));
        Map<Long, Long> ghOwner = ghs.stream().collect(Collectors.toMap(Greenhouse::getId, Greenhouse::getOwnerId, (a, b) -> a));

        for (Alert a : alerts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("content", a.getContent());
            item.put("level", a.getLevel().name());
            item.put("sensorType", a.getSensorType());
            item.put("createdAt", a.getCreatedAt());
            item.put("greenhouseName", ghName.getOrDefault(a.getGreenhouseId(), "未知"));
            item.put("ownerId", ghOwner.getOrDefault(a.getGreenhouseId(), 0L));
            result.add(item);
        }
        return result;
    }

    // ===== 6. 当前天气（地区级，取最深一级地区名） =====

    private Object buildWeather(String province, String city, String district, String town, String village) {
        String location = pick(village, town, district, city, province);
        if (location == null) {
            location = "北京";
        }
        try {
            WeatherCurrentResponse weather = weatherService.getCurrentWeather(location);
            return weather;
        } catch (Exception e) {
            log.warn("地区天气查询失败 location={}: {}", location, e.getMessage());
            return null;
        }
    }

    // ===== 工具 =====

    private String pick(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
