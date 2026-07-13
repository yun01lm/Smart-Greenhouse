package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 预警规则实体
 * <p>
 * 对应数据库第 9 号表 alert_rules。
 * 定义触发告警的条件：传感器类型 + 阈值/趋势/复合规则。
 * </p>
 */
@Entity
@Table(name = "alert_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 传感器组ID（可空，为空表示大棚级别规则） */
    @Column(name = "group_id")
    private Long groupId;

    /** 传感器类型 */
    @Column(name = "sensor_type", nullable = false, length = 30)
    private String sensorType;

    /**
     * 规则类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private RuleType ruleType;

    /**
     * 规则条件 JSON
     * THRESHOLD: {"min":15,"max":35}
     * TREND: {"direction":"rising","rate":5,"duration":300}
     * COMPOSITE: {"conditions":[...],"logic":"AND"}
     */
    @Column(name = "condition_json", columnDefinition = "JSON", nullable = false)
    private String conditionJson;

    /**
     * 告警级别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level", nullable = false, length = 10)
    private AlertLevel alertLevel;

    /** 关联场景ID（告警触发时可自动执行场景） */
    @Column(name = "scene_id")
    private Long sceneId;

    /** 是否启用 */
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== 枚举 =====

    public enum RuleType {
        /** 阈值规则（超过上下限触发） */
        THRESHOLD,
        /** 趋势规则（连续上升/下降触发） */
        TREND,
        /** 复合规则（多条件组合） */
        COMPOSITE,
        /** 天气关联规则（和风天气数据联动） */
        WEATHER
    }

    public enum AlertLevel {
        /** 提示 */
        INFO,
        /** 警告 */
        WARNING,
        /** 严重 */
        CRITICAL
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
