package com.greenhouse.module.health.dto;

import com.greenhouse.entity.HealthAssessment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康评分历史条目 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthHistoryResponse {

    private Long id;
    private BigDecimal overallScore;
    private BigDecimal envScore;
    private BigDecimal visualScore;
    private String weatherRisk;
    private String level;
    private String levelColor;
    private LocalDateTime createdAt;

    public static HealthHistoryResponse fromEntity(HealthAssessment entity) {
        HealthAssessment.ScoreLevel level = HealthAssessment.ScoreLevel.fromScore(
                entity.getOverallScore() != null ? entity.getOverallScore().doubleValue() : 0);

        return HealthHistoryResponse.builder()
                .id(entity.getId())
                .overallScore(entity.getOverallScore())
                .envScore(entity.getEnvScore())
                .visualScore(entity.getVisualScore())
                .weatherRisk(entity.getWeatherRisk())
                .level(level.getLabel())
                .levelColor(level.getColor())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
