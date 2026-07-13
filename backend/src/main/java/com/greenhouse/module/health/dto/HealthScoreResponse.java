package com.greenhouse.module.health.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.entity.HealthAssessment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康评分响应 DTO
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreResponse {

    private Long id;
    private Long greenhouseId;
    private BigDecimal overallScore;
    private String level;
    private String levelColor;
    private BigDecimal envScore;
    private BigDecimal visualScore;
    private String weatherRisk;
    private BigDecimal weatherFactor;
    private Map<String, Object> analysis;
    private String recommendations;
    private LocalDateTime createdAt;

    public static HealthScoreResponse fromEntity(HealthAssessment entity) {
        HealthAssessment.ScoreLevel level = HealthAssessment.ScoreLevel.fromScore(
                entity.getOverallScore() != null ? entity.getOverallScore().doubleValue() : 0);

        Map<String, Object> analysis = null;
        if (entity.getAnalysisJson() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> map = mapper.readValue(entity.getAnalysisJson(), Map.class);
                analysis = map;
            } catch (JsonProcessingException e) {
                log.warn("解析分析JSON失败: assessmentId={}", entity.getId());
            }
        }

        return HealthScoreResponse.builder()
                .id(entity.getId())
                .greenhouseId(entity.getGreenhouseId())
                .overallScore(entity.getOverallScore())
                .level(level.getLabel())
                .levelColor(level.getColor())
                .envScore(entity.getEnvScore())
                .visualScore(entity.getVisualScore())
                .weatherRisk(entity.getWeatherRisk())
                .weatherFactor(entity.getWeatherFactor())
                .analysis(analysis)
                .recommendations(entity.getRecommendations())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
