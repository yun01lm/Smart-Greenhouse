package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 长势评估记录实体
 * <p>
 * 对应数据库第 17 号表 growth_assessments。
 * 存储作物长势评估记录，关联生长周期，包含株高、叶面积、叶色等指标。
 * </p>
 */
@Entity
@Table(name = "growth_assessments", indexes = {
        @Index(name = "idx_greenhouse_created", columnList = "greenhouse_id, created_at"),
        @Index(name = "idx_crop_cycle", columnList = "crop_cycle_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 生长周期ID（可空） */
    @Column(name = "crop_cycle_id")
    private Long cropCycleId;

    /** 截帧图片路径 */
    @Column(name = "image_path", length = 255)
    private String imagePath;

    /** 生长阶段：苗期/生长期/开花期/结果期/收获期 */
    @Column(name = "growth_stage", length = 50)
    private String growthStage;

    /** 株高（cm） */
    @Column(name = "plant_height", precision = 10, scale = 2)
    private BigDecimal plantHeight;

    /** 叶面积（cm²） */
    @Column(name = "leaf_area", precision = 10, scale = 2)
    private BigDecimal leafArea;

    /** 叶色 */
    @Column(name = "leaf_color", length = 50)
    private String leafColor;

    /** 健康评分（0-100） */
    @Column(name = "health_score", precision = 5, scale = 2)
    private BigDecimal healthScore;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
