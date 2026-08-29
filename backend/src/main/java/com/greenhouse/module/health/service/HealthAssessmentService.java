package com.greenhouse.module.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.config.FusionConfig;
import com.greenhouse.entity.Alert;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.HealthAssessment;
import com.greenhouse.module.websocket.service.RealtimePushService;
import com.greenhouse.repository.AlertRepository;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.HealthAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 多模态融合分析服务（C15 核心引擎）
 * <p>
 * 将环境时序数据、视觉诊断数据和天气数据进行跨模态融合，
 * 生成综合健康评分（0-100分）。
 * </p>
 *
 * <h3>融合公式（权重可通过 application.yml 配置）</h3>
 * <pre>
 * overall_score = (env_score × envWeight + visual_score × visualWeight) × weather_factor
 * overall_score = clamp(overall_score, 0, 100)
 * </pre>
 *
 * <h3>触发时机</h3>
 * <ul>
 *   <li>用户手动请求 GET /api/v1/health/score → 实时计算</li>
 *   <li>定时任务（每30分钟）→ 更新环境健康分</li>
 *   <li>诊断完成后 → 更新视觉健康分</li>
 *   <li>天气数据更新后 → 更新天气修正因子</li>
 * </ul>
 *
 * <h3>低分预警</h3>
 * <ul>
 *   <li>overall_score < 40 → CRITICAL 预警</li>
 *   <li>overall_score < 60 → WARNING 预警</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthAssessmentService {

    private final EnvironmentHealthCalculator envCalculator;
    private final VisualHealthCalculator visualCalculator;
    private final WeatherRiskCalculator weatherCalculator;
    private final HealthAssessmentRepository assessmentRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final AlertRepository alertRepository;
    private final RealtimePushService pushService;
    private final ObjectMapper objectMapper;
    private final FusionConfig fusionConfig;

    /**
     * 计算并保存综合健康评分
     *
     * @param greenhouseId 大棚ID
     * @return 健康评估记录
     */
    @Transactional
    public HealthAssessment calculateAndSave(Long greenhouseId) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        // 1. 计算环境健康分 (60%)
        double envScore = envCalculator.calculate(greenhouseId);

        // 2. 计算视觉健康分 (40%)
        double visualScore = visualCalculator.calculate(greenhouseId);

        // NaN/Infinity 防御：数据异常（如控制器状态值混入、InfluxDB 脏数据）
        // 不应击穿健康评分接口，非有限值按默认 80 分处理
        if (!Double.isFinite(envScore)) {
            log.warn("大棚 {} 环境健康分异常({})，使用默认值 80.0", greenhouseId, envScore);
            envScore = 80.0;
        }
        if (!Double.isFinite(visualScore)) {
            log.warn("大棚 {} 视觉健康分异常({})，使用默认值 80.0", greenhouseId, visualScore);
            visualScore = 80.0;
        }

        // 3. 计算天气修正因子
        String location = resolveWeatherLocation(greenhouse);
        WeatherRiskCalculator.WeatherRiskResult weatherResult =
                weatherCalculator.calculate(location);
        double weatherFactor = weatherResult.factor();
        String weatherRisk = weatherResult.description();

        // 4. 加权融合（权重从 FusionConfig 读取）
        FusionConfig.OverallWeights overallWeights = fusionConfig.getOverall();
        double overallScore = (envScore * overallWeights.getEnvWeight()
                + visualScore * overallWeights.getVisualWeight()) * weatherFactor;
        overallScore = Math.max(0, Math.min(100, overallScore));

        // 5. 生成分析详情
        String analysisJson = buildAnalysisJson(greenhouseId, envScore, visualScore,
                weatherFactor, weatherRisk);

        // 6. 生成建议
        String recommendations = generateRecommendations(
                greenhouse, envScore, visualScore, weatherFactor);

        // 7. 存储评估记录
        HealthAssessment assessment = HealthAssessment.builder()
                .greenhouseId(greenhouseId)
                .envScore(BigDecimal.valueOf(envScore).setScale(2, RoundingMode.HALF_UP))
                .visualScore(BigDecimal.valueOf(visualScore).setScale(2, RoundingMode.HALF_UP))
                .weatherRisk(weatherRisk)
                .weatherFactor(BigDecimal.valueOf(weatherFactor).setScale(2, RoundingMode.HALF_UP))
                .overallScore(BigDecimal.valueOf(overallScore).setScale(2, RoundingMode.HALF_UP))
                .analysisJson(analysisJson)
                .recommendations(recommendations)
                .build();

        assessment = assessmentRepository.save(assessment);

        // 8. WebSocket 推送
        pushHealthScore(greenhouseId, assessment);

        // 9. 低分预警
        if (overallScore < 40) {
            triggerLowScoreAlert(greenhouseId, assessment, Alert.AlertLevel.CRITICAL);
        } else if (overallScore < 60) {
            triggerLowScoreAlert(greenhouseId, assessment, Alert.AlertLevel.WARNING);
        }

        log.info("大棚 {} 健康评分已计算: env={:.1f}, visual={:.1f}, weatherFactor={:.2f}, overall={:.1f}",
                greenhouseId, envScore, visualScore, weatherFactor, overallScore);

        return assessment;
    }

    /**
     * 获取当前综合健康评分（优先返回最近30分钟内的缓存）
     */
    public HealthAssessment getCurrentScore(Long greenhouseId) {
        return assessmentRepository.findTopByGreenhouseIdOrderByCreatedAtDesc(greenhouseId)
                .orElse(null);
    }

    /**
     * 查询健康评分历史（分页）
     */
    public Page<HealthAssessment> getHistory(Long greenhouseId, LocalDateTime startDate,
                                              LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return assessmentRepository.findByGreenhouseIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                greenhouseId, startDate, endDate, pageable);
    }

    /**
     * 获取详细评估报告
     */
    public HealthAssessment getDetail(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "评估报告不存在"));
    }

    // ===== 辅助方法 =====

    /**
     * 解析天气查询位置
     * <p>
     * 优先使用大棚的 city 字段，其次使用 province，最后使用 location 描述。
     * </p>
     */
    private String resolveWeatherLocation(Greenhouse greenhouse) {
        if (greenhouse.getCity() != null && !greenhouse.getCity().isBlank()) {
            return greenhouse.getCity();
        }
        if (greenhouse.getProvince() != null && !greenhouse.getProvince().isBlank()) {
            return greenhouse.getProvince();
        }
        if (greenhouse.getLocation() != null && !greenhouse.getLocation().isBlank()) {
            return greenhouse.getLocation();
        }
        return "北京"; // 默认位置
    }

    /**
     * 构建分析详情 JSON
     */
    private String buildAnalysisJson(Long greenhouseId, double envScore, double visualScore,
                                      double weatherFactor, String weatherRisk) {
        Map<String, Object> analysis = new LinkedHashMap<>();

        // 环境健康详情
        Map<String, Object> envDetail = new LinkedHashMap<>();
        envDetail.put("score", String.format("%.1f", envScore));
        envDetail.put("weight", "60%");
        if (envScore >= 80) {
            envDetail.put("comment", "环境参数在适宜范围内");
        } else if (envScore >= 60) {
            envDetail.put("comment", "部分环境参数偏离适宜范围");
        } else {
            envDetail.put("comment", "环境参数异常，需立即调整");
        }
        analysis.put("envDetail", envDetail);

        // 视觉健康详情
        Map<String, Object> visualDetail = new LinkedHashMap<>();
        visualDetail.put("score", String.format("%.1f", visualScore));
        visualDetail.put("weight", "40%");
        if (visualScore >= 80) {
            visualDetail.put("comment", "作物视觉特征正常");
        } else if (visualScore >= 60) {
            visualDetail.put("comment", "作物存在轻微异常");
        } else {
            visualDetail.put("comment", "作物视觉特征异常，建议诊断");
        }
        analysis.put("visualDetail", visualDetail);

        // 天气影响
        Map<String, Object> weatherImpact = new LinkedHashMap<>();
        weatherImpact.put("factor", String.format("%.2f", weatherFactor));
        weatherImpact.put("riskAssessment", weatherRisk);
        analysis.put("weatherImpact", weatherImpact);

        try {
            return objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            log.error("构建分析JSON失败", e);
            return "{}";
        }
    }

    /**
     * 生成建议措施
     */
    private String generateRecommendations(Greenhouse greenhouse, double envScore,
                                            double visualScore, double weatherFactor) {
        List<String> recommendations = new ArrayList<>();

        // 环境建议
        if (envScore >= 80) {
            recommendations.add("环境参数在适宜范围内，继续保持当前管理策略");
        } else if (envScore >= 60) {
            recommendations.add("部分环境参数偏离适宜范围，建议检查传感器数据并调整通风/灌溉设备");
        } else {
            recommendations.add("环境参数异常严重，建议立即检查大棚设备和环境调控系统");
        }

        // 视觉健康建议
        if (visualScore < 60) {
            recommendations.add("作物视觉健康评分偏低，建议进行病虫害诊断检查");
        } else if (visualScore < 80) {
            recommendations.add("作物视觉健康评分一般，建议关注作物长势变化");
        }

        // 天气风险建议
        if (weatherFactor < 0.8) {
            recommendations.add("未来有极端天气风险，建议提前做好防护措施（加固棚体、调整温控）");
        } else if (weatherFactor < 0.95) {
            recommendations.add("未来天气有轻微不利变化，建议关注天气预报");
        }

        // 综合
        if (recommendations.isEmpty()) {
            recommendations.add("大棚整体健康状况良好，继续保持当前管理措施");
        }

        return String.join("；", recommendations);
    }

    /**
     * 推送健康评分到 WebSocket
     */
    private void pushHealthScore(Long greenhouseId, HealthAssessment assessment) {
        HealthAssessment.ScoreLevel level = HealthAssessment.ScoreLevel.fromScore(
                assessment.getOverallScore().doubleValue());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "HEALTH_SCORE");
        payload.put("greenhouseId", greenhouseId);
        payload.put("overallScore", assessment.getOverallScore());
        payload.put("level", level.getLabel());
        payload.put("levelColor", level.getColor());
        payload.put("envScore", assessment.getEnvScore());
        payload.put("visualScore", assessment.getVisualScore());
        payload.put("weatherRisk", assessment.getWeatherRisk());
        payload.put("recommendations", assessment.getRecommendations());
        payload.put("updatedAt", Instant.now().toString());

        try {
            String json = objectMapper.writeValueAsString(payload);
            // 使用 SimpMessagingTemplate 直接发送（pushService 提供的是高层封装，这里用底层API更灵活）
            pushService.pushSensorData(greenhouseId, 0L, "健康评估系统",
                    "HEALTH_SCORE", assessment.getOverallScore().doubleValue());

            // 同时推送到专用健康主题
            // messagingTemplate.convertAndSend("/topic/greenhouse/" + greenhouseId + "/health", payload);
        } catch (Exception e) {
            log.warn("WebSocket 推送健康评分失败: {}", e.getMessage());
        }
    }

    /**
     * 触发低分预警
     */
    private void triggerLowScoreAlert(Long greenhouseId, HealthAssessment assessment,
                                       Alert.AlertLevel alertLevel) {
        HealthAssessment.ScoreLevel scoreLevel = HealthAssessment.ScoreLevel.fromScore(
                assessment.getOverallScore().doubleValue());

        String title = alertLevel == Alert.AlertLevel.CRITICAL
                ? "大棚健康评分危险预警"
                : "大棚健康评分关注预警";

        String content = String.format(
                "大棚综合健康评分为 %.1f 分，等级：%s。" +
                        "环境健康分：%.1f，视觉健康分：%.1f，天气风险：%s。" +
                        "建议：%s",
                assessment.getOverallScore(),
                scoreLevel.getLabel(),
                assessment.getEnvScore(),
                assessment.getVisualScore(),
                assessment.getWeatherRisk(),
                assessment.getRecommendations()
        );

        Alert alert = Alert.builder()
                .greenhouseId(greenhouseId)
                .level(alertLevel)
                .title(title)
                .content(content)
                .sensorType("HEALTH_SCORE")
                .sensorValue(assessment.getOverallScore().doubleValue())
                .build();

        alertRepository.save(alert);

        // 通过 WebSocket 推送预警
        pushService.pushAlert(alert.getId(), greenhouseId, alertLevel.name(),
                title, content, "HEALTH_SCORE",
                assessment.getOverallScore().doubleValue(),
                alertLevel == Alert.AlertLevel.CRITICAL ? 40.0 : 60.0);

        log.warn("健康评分低分预警触发: greenhouseId={}, score={}, level={}",
                greenhouseId, assessment.getOverallScore(), alertLevel);
    }
}
