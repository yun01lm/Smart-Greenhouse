package com.greenhouse.module.crop.dto;

import com.greenhouse.entity.CropCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 作物生长周期响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropCycleResponse {

    /** 周期ID */
    private Long id;

    /** 大棚ID */
    private Long greenhouseId;

    /** 作物名称 */
    private String cropType;

    /** 品种 */
    private String variety;

    /** 种植日期 */
    private LocalDate plantingDate;

    /** 预计收获日期 */
    private LocalDate expectedHarvestDate;

    /** 实际收获日期 */
    private LocalDate actualHarvestDate;

    /** 当前阶段 */
    private String currentStage;

    /** 阶段来源（AUTO/MANUAL） */
    private String stageSource;

    /** 状态（ACTIVE/COMPLETED/CANCELLED） */
    private String status;

    /** 种植后天数 */
    private long daysSincePlanting;

    /** 备注 */
    private String notes;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static CropCycleResponse fromEntity(CropCycle cycle) {
        return CropCycleResponse.builder()
                .id(cycle.getId())
                .greenhouseId(cycle.getGreenhouseId())
                .cropType(cycle.getCropType())
                .variety(cycle.getVariety())
                .plantingDate(cycle.getPlantingDate())
                .expectedHarvestDate(cycle.getExpectedHarvestDate())
                .actualHarvestDate(cycle.getActualHarvestDate())
                .currentStage(cycle.getCurrentStage())
                .stageSource(cycle.getStageSource().name())
                .status(cycle.getStatus().name())
                .daysSincePlanting(cycle.getDaysSincePlanting())
                .notes(cycle.getNotes())
                .createdAt(cycle.getCreatedAt())
                .build();
    }
}
