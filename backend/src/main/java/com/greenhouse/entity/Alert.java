package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 预警记录实体
 * <p>
 * 对应数据库第 10 号表 alerts。
 * 每次规则匹配成功后自动生成一条告警记录。
 * </p>
 */
@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 传感器组ID（可空） */
    @Column(name = "group_id")
    private Long groupId;

    /** 触发的规则ID（可空，自定义阈值触发时为空） */
    @Column(name = "alert_rule_id")
    private Long alertRuleId;

    /**
     * 告警级别
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertLevel level;

    /** 告警标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 告警内容 */
    @Column(nullable = false, length = 1000)
    private String content;

    /** 触发告警的传感器数值 */
    @Column(name = "sensor_value")
    private Double sensorValue;

    /** 传感器类型 */
    @Column(name = "sensor_type", length = 30)
    private String sensorType;

    /** 天气信息（天气关联告警时使用） */
    @Column(name = "weather_info", length = 500)
    private String weatherInfo;

    /** 是否已读 */
    @Column(name = "read_status", nullable = false)
    @Builder.Default
    private Boolean readStatus = false;

    /** 是否已处理 */
    @Column(name = "handled", nullable = false)
    @Builder.Default
    private Boolean handled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== 枚举 =====

    public enum AlertLevel {
        INFO, WARNING, CRITICAL
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
