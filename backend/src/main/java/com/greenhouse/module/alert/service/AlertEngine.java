package com.greenhouse.module.alert.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.entity.Alert;
import com.greenhouse.entity.AlertRule;
import com.greenhouse.entity.UserAlertThreshold;
import com.greenhouse.module.websocket.service.RealtimePushService;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 预警引擎
 * <p>
 * 核心职责：MQTT 收到传感器数据后，比对预警规则和用户自定义阈值，
 * 如果超出阈值则自动生成告警记录并通过 WebSocket 推送。
 * </p>
 *
 * <h3>检测流程</h3>
 * <ol>
 *   <li>查找该大棚 + 传感器类型的所有启用规则</li>
 *   <li>查找该大棚下所有用户自定义阈值</li>
 *   <li>逐一比对：THRESHOLD 规则检查 min/max，TREND 规则暂存数据（Phase 2 后期完善）</li>
 *   <li>超阈值 → 生成 Alert 记录 → WebSocket 推送 → 可选触发场景</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEngine {

    private final AlertRuleRepository ruleRepository;
    private final AlertRepository alertRepository;
    private final UserAlertThresholdRepository thresholdRepository;
    private final RealtimePushService pushService;
    private final ObjectMapper objectMapper;

    /**
     * 检测传感器数据是否触发预警
     * <p>
     * 由 MqttSubscriber 在收到每条传感器数据后调用。
     * </p>
     *
     * @param greenhouseId 大棚ID
     * @param deviceId     设备ID
     * @param sensorType   传感器类型
     * @param value        传感器数值
     */
    public void check(Long greenhouseId, Long deviceId, String sensorType, Double value) {
        // 1. 检查系统预警规则
        List<AlertRule> rules = ruleRepository.findByGreenhouseIdAndSensorTypeAndEnabledTrue(
                greenhouseId, sensorType);

        for (AlertRule rule : rules) {
            try {
                if (rule.getRuleType() == AlertRule.RuleType.THRESHOLD) {
                    checkThresholdRule(rule, greenhouseId, value);
                }
                // TREND / COMPOSITE / WEATHER 规则留给后续步骤完善
                } else if (rule.getRuleType() == AlertRule.RuleType.WEATHER) {
                    checkWeatherRule(rule, greenhouseId, sensorType, value);
            } catch (Exception e) {
                log.warn("预警规则检测异常: ruleId={}, error={}", rule.getId(), e.getMessage());
            }
        }

        // 2. 检查用户自定义阈值
        List<UserAlertThreshold> thresholds = thresholdRepository.findByGreenhouseId(greenhouseId);
        for (UserAlertThreshold threshold : thresholds) {
            if (!threshold.getEnabled() || !threshold.getSensorType().equals(sensorType)) {
                continue;
            }
            try {
                checkUserThreshold(threshold, greenhouseId, value);
            } catch (Exception e) {
                log.warn("自定义阈值检测异常: thresholdId={}, error={}",
                        threshold.getId(), e.getMessage());
            }
        }
    }

    /**
     * 阈值规则检测
     */
    private void checkThresholdRule(AlertRule rule, Long greenhouseId, Double value) throws Exception {
        JsonNode condition = objectMapper.readTree(rule.getConditionJson());

        Double min = condition.has("min") && !condition.get("min").isNull()
                ? condition.get("min").asDouble() : null;
        Double max = condition.has("max") && !condition.get("max").isNull()
                ? condition.get("max").asDouble() : null;

        String alertTitle = null;
        String alertContent = null;

        if (min != null && value < min) {
            alertTitle = String.format("「%s」数值过低", rule.getSensorType());
            alertContent = String.format("%s 当前值 %.1f，低于最低阈值 %.1f",
                    rule.getSensorType(), value, min);
        } else if (max != null && value > max) {
            alertTitle = String.format("「%s」数值过高", rule.getSensorType());
            alertContent = String.format("%s 当前值 %.1f，超过最高阈值 %.1f",
                    rule.getSensorType(), value, max);
        }

        if (alertTitle != null) {
            createAlert(greenhouseId, rule.getGroupId(), rule.getId(),
                    rule.getAlertLevel(), alertTitle, alertContent,
                    value, rule.getSensorType());
        }
    }

    /**
     * 用户自定义阈值检测
     */
    private void checkUserThreshold(UserAlertThreshold threshold, Long greenhouseId, Double value) {
        String alertTitle = null;
        String alertContent = null;
        AlertRule.AlertLevel level = AlertRule.AlertLevel.WARNING;

        if (threshold.getMinThreshold() != null && value < threshold.getMinThreshold()) {
            alertTitle = String.format("「%s」低于自定义阈值", threshold.getSensorType());
            alertContent = String.format("%s 当前值 %.1f，低于您设置的阈值 %.1f",
                    threshold.getSensorType(), value, threshold.getMinThreshold());
        } else if (threshold.getMaxThreshold() != null && value > threshold.getMaxThreshold()) {
            alertTitle = String.format("「%s」超过自定义阈值", threshold.getSensorType());
            alertContent = String.format("%s 当前值 %.1f，超过您设置的阈值 %.1f",
                    threshold.getSensorType(), value, threshold.getMaxThreshold());
        }

        if (alertTitle != null) {
            createAlert(greenhouseId, threshold.getGroupId(), null,
                    level, alertTitle, alertContent, value, threshold.getSensorType());
        }
    }

    /**
     * 创建告警记录 + WebSocket 推送
     */
    /**
     * 天气关联规则检测（传感器数据触发）
     */
    private void checkWeatherRule(AlertRule rule, Long greenhouseId, String sensorType, Double value) {
        try {
            Greenhouse gh = greenhouseRepository.findById(greenhouseId).orElse(null);
            if (gh == null || gh.getCity() == null || gh.getCity().isBlank()) return;

            WeatherCurrentResponse weather = weatherService.getCurrentWeather(gh.getCity());

            if ("TEMPERATURE".equalsIgnoreCase(sensorType)) {
                if (value > 35 && weather.getTemperature().doubleValue() > 33) {
                    createAlert(greenhouseId, rule.getGroupId(), rule.getId(),
                            AlertRule.AlertLevel.WARNING, "高温天气预警",
                            String.format("棚内温度 %.1f°C，室外温度 %s°C，请注意通风降温", value, weather.getTemperature()),
                            value, sensorType);
                }
                if (value < 10 && weather.getTemperature().doubleValue() < 8) {
                    createAlert(greenhouseId, rule.getGroupId(), rule.getId(),
                            AlertRule.AlertLevel.WARNING, "低温天气预警",
                            String.format("棚内温度 %.1f°C，室外温度 %s°C，请注意保温", value, weather.getTemperature()),
                            value, sensorType);
                }
            }
            if ("HUMIDITY".equalsIgnoreCase(sensorType)) {
                boolean isRainy = weather.getWeatherCode() != null
                        && (weather.getWeatherCode().startsWith("3") || weather.getWeatherCode().startsWith("4"));
                if (value > 90 && isRainy) {
                    createAlert(greenhouseId, rule.getGroupId(), rule.getId(),
                            AlertRule.AlertLevel.WARNING, "高湿+降雨预警",
                            String.format("棚内湿度 %.1f%% + 室外天气 %s，请注意防涝", value, weather.getWeatherText()),
                            value, sensorType);
                }
            }
        } catch (Exception e) {
            log.warn("天气规则检测异常: ruleId={}, greenhouseId={}, error={}", rule.getId(), greenhouseId, e.getMessage());
        }
    }

    /**
     * 定时天气巡检（每 30 分钟）
     */
    @Scheduled(fixedDelay = 1800000)
    public void scheduledWeatherCheck() {
        try {
            List<AlertRule> weatherRules = ruleRepository.findByRuleTypeAndEnabledTrue(AlertRule.RuleType.WEATHER);
            if (weatherRules.isEmpty()) return;

            for (AlertRule rule : weatherRules) {
                try {
                    Greenhouse gh = greenhouseRepository.findById(rule.getGreenhouseId()).orElse(null);
                    if (gh == null || gh.getCity() == null || gh.getCity().isBlank()) continue;

                    WeatherCurrentResponse weather = weatherService.getCurrentWeather(gh.getCity());

                    if (weather.getTemperature().doubleValue() > 38) {
                        createAlert(rule.getGreenhouseId(), rule.getGroupId(), rule.getId(),
                                AlertRule.AlertLevel.CRITICAL, "极端高温天气",
                                String.format("室外温度达 %s°C（%s），请采取紧急降温措施", weather.getTemperature(), gh.getCity()),
                                weather.getTemperature().doubleValue(), "WEATHER");
                    }
                    if (weather.getTemperature().doubleValue() < 0) {
                        createAlert(rule.getGreenhouseId(), rule.getGroupId(), rule.getId(),
                                AlertRule.AlertLevel.CRITICAL, "霜冻预警",
                                String.format("室外温度 %s°C（%s），请立即启动加温设备", weather.getTemperature(), gh.getCity()),
                                weather.getTemperature().doubleValue(), "WEATHER");
                    }
                    if (weather.getWindSpeed().doubleValue() > 10.7) {
                        createAlert(rule.getGreenhouseId(), rule.getGroupId(), rule.getId(),
                                AlertRule.AlertLevel.WARNING, "大风预警",
                                String.format("风速 %s m/s（%s），请检查大棚结构安全", weather.getWindSpeed(), gh.getCity()),
                                weather.getWindSpeed().doubleValue(), "WEATHER");
                    }
                    if (weather.getWeatherCode() != null
                            && (weather.getWeatherCode().startsWith("4") || "302".equals(weather.getWeatherCode()))) {
                        createAlert(rule.getGreenhouseId(), rule.getGroupId(), rule.getId(),
                                AlertRule.AlertLevel.WARNING, "暴雨预警",
                                String.format("天气 %s（%s），请检查排水系统", weather.getWeatherText(), gh.getCity()),
                                0.0, "WEATHER");
                    }
                } catch (Exception e) {
                    log.warn("定时天气巡检异常: ruleId={}, error={}", rule.getId(), e.getMessage());
                }
            }
            log.debug("定时天气巡检完成: 检测 {} 条规则", weatherRules.size());
        } catch (Exception e) {
            log.error("定时天气巡检失败: {}", e.getMessage(), e);
        }
    }
    private void createAlert(Long greenhouseId, Long groupId, Long ruleId,
                              AlertRule.AlertLevel level, String title, String content,
                              Double sensorValue, String sensorType) {
        Alert.AlertLevel alertLevel = Alert.AlertLevel.valueOf(level.name());

        Alert alert = Alert.builder()
                .greenhouseId(greenhouseId)
                .groupId(groupId)
                .alertRuleId(ruleId)
                .level(alertLevel)
                .title(title)
                .content(content)
                .sensorValue(sensorValue)
                .sensorType(sensorType)
                .readStatus(false)
                .build();

        alert = alertRepository.save(alert);

        // WebSocket 推送告警
        pushService.pushAlert(alert.getId(), greenhouseId, level.name(),
                title, content, sensorType, sensorValue, null);

        log.info("预警触发: id={}, level={}, title={}, value={}",
                alert.getId(), level, title, sensorValue);
    }
}
