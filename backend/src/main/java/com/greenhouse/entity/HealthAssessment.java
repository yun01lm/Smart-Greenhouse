package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 多模态健康综合评估实体
 * <p>
 * 对应 DB 第 18 号表 health_assessments。
 * 存储环境+图像融合后的综合健康评分，每次计算生成一条记录。
 * </p>
 *
 * <h3>评分等级</h3>
 * <ul>
 *   <li>80-100: 健康 (绿色)</li>
 *   <li>60-79: 良好 (蓝色)</li>
 *   <li>40-59: 关注 (黄色)</li>
 *   <li>20-39: 警告 (橙色)</li>
 *   <li>0-19: 危险 (红色)</li>
 * </ul>
 */
@Entity
@Table(name = "health_assessments", indexes = {
        @Index(name = "idx_greenhouse_created", columnList = "greenhouse_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 环境健康分 (0-100) */
    @Column(name = "env_score", precision = 5, scale = 2)
    private BigDecimal envScore;

    /** 视觉健康分 (0-100) */
    @Column(name = "visual_score", precision = 5, scale = 2)
    private BigDecimal visualScore;

    /** 天气风险等级 */
    @Column(name = "weather_risk", length = 50)
    private String weatherRisk;

    /** 天气修正因子 */
    @Column(name = "weather_factor", precision = 3, scale = 2)
    private BigDecimal weatherFactor;

    /** 综合健康评分 (0-100) */
    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    /** 详细分析数据（JSON） */
    @Column(name = "analysis_json", columnDefinition = "JSON")
    private String analysisJson;

    /** 建议措施 */
    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== 枚举 =====

    /** 评分等级 */
    public enum ScoreLevel {
        HEALTHY("健康", "green", 80),
        GOOD("良好", "blue", 60),
        ATTENTION("关注", "yellow", 40),
        WARNING("警告", "orange", 20),
        DANGER("危险", "red", 0);

        private final String label;
        private final String color;
        private final int minScore;

        ScoreLevel(String label, String color, int minScore) {
            this.label = label;
            this.color = color;
            this.minScore = minScore;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }

        /**
         * 根据评分返回对应等级
         */
        public static ScoreLevel fromScore(double score) {
            if (score >= 80) return HEALTHY;
            if (score >= 60) return GOOD;
            if (score >= 40) return ATTENTION;
            if (score >= 20) return WARNING;
            return DANGER;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
