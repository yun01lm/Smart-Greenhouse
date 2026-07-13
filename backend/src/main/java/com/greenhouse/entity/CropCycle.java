package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 作物生长周期实体
 * <p>
 * 对应 DB 第 20 号表 crop_cycles。
 * 管理每个大棚每轮种植的完整生长周期，支持阶段自动估算和手动修正。
 * </p>
 *
 * <h3>生长阶段</h3>
 * 育苗期 → 生长期 → 开花期 → 结果期 → 收获期
 *
 * <h3>自动估算逻辑</h3>
 * 根据种植日期和作物类型的标准周期自动计算当前阶段，
 * 用户可通过 stageSource=MANUAL 覆盖。
 * </p>
 */
@Entity
@Table(name = "crop_cycles", indexes = {
        @Index(name = "idx_greenhouse_status", columnList = "greenhouse_id, status"),
        @Index(name = "idx_crop_type", columnList = "crop_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 作物名称 */
    @Column(name = "crop_type", nullable = false, length = 50)
    private String cropType;

    /** 品种 */
    @Column(name = "variety", length = 50)
    private String variety;

    /** 种植日期 */
    @Column(name = "planting_date", nullable = false)
    private LocalDate plantingDate;

    /** 预计收获日期 */
    @Column(name = "expected_harvest_date")
    private LocalDate expectedHarvestDate;

    /** 实际收获日期 */
    @Column(name = "actual_harvest_date")
    private LocalDate actualHarvestDate;

    /** 当前阶段 */
    @Column(name = "current_stage", nullable = false, length = 20)
    @Builder.Default
    private String currentStage = "育苗期";

    /** 阶段来源 */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage_source", nullable = false, length = 10)
    @Builder.Default
    private StageSource stageSource = StageSource.AUTO;

    /** 状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private CycleStatus status = CycleStatus.ACTIVE;

    /** 备注 */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== 枚举 =====

    /** 阶段来源 */
    public enum StageSource {
        AUTO,    // 自动估算
        MANUAL   // 手动设置
    }

    /** 周期状态 */
    public enum CycleStatus {
        ACTIVE,      // 进行中
        COMPLETED,   // 已收获
        CANCELLED    // 已取消
    }

    /** 标准生长阶段 */
    public static final String[] STANDARD_STAGES = {
            "育苗期", "生长期", "开花期", "结果期", "收获期"
    };

    /**
     * 根据种植天数自动估算当前阶段
     * <p>
     * 各阶段默认天数（可调整）：
     * 育苗期 0-20天、生长期 21-40天、开花期 41-55天、结果期 56-80天、收获期 81天+
     * </p>
     *
     * @param daysSincePlanting 种植后天数
     * @return 预估的当前阶段
     */
    public static String estimateStage(long daysSincePlanting) {
        if (daysSincePlanting <= 20) return STANDARD_STAGES[0];  // 育苗期
        if (daysSincePlanting <= 40) return STANDARD_STAGES[1];  // 生长期
        if (daysSincePlanting <= 55) return STANDARD_STAGES[2];  // 开花期
        if (daysSincePlanting <= 80) return STANDARD_STAGES[3];  // 结果期
        return STANDARD_STAGES[4];  // 收获期
    }

    /**
     * 计算种植后天数
     */
    public long getDaysSincePlanting() {
        if (plantingDate == null) return 0;
        return ChronoUnit.DAYS.between(plantingDate, LocalDate.now());
    }

    /**
     * 自动更新阶段（仅在 AUTO 模式下）
     */
    public void autoUpdateStage() {
        if (stageSource == StageSource.AUTO) {
            long days = getDaysSincePlanting();
            this.currentStage = estimateStage(days);
        }
    }
}
