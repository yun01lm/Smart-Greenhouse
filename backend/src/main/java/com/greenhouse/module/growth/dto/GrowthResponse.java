package com.greenhouse.module.growth.dto;

import com.greenhouse.entity.GrowthAssessment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 长势评估响应 DTO
 */
@Data
@Builder
public class GrowthResponse {

    private Long id;
    private Long greenhouseId;
    private Long cropCycleId;
    private String imagePath;
    private String growthStage;
    private BigDecimal plantHeight;
    private BigDecimal leafArea;
    private String leafColor;
    private BigDecimal healthScore;
    private LocalDateTime createdAt;

    public static GrowthResponse fromEntity(GrowthAssessment entity) {
        return GrowthResponse.builder()
                .id(entity.getId())
                .greenhouseId(entity.getGreenhouseId())
                .cropCycleId(entity.getCropCycleId())
                .imagePath(entity.getImagePath())
                .growthStage(entity.getGrowthStage())
                .plantHeight(entity.getPlantHeight())
                .leafArea(entity.getLeafArea())
                .leafColor(entity.getLeafColor())
                .healthScore(entity.getHealthScore())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
