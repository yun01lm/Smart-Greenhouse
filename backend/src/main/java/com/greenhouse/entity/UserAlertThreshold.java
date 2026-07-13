package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户自定义预警阈值实体
 * <p>
 * 对应数据库第 25 号表 user_alert_thresholds。
 * 用户可以覆盖系统默认的预警规则，设置自己的阈值。
 * </p>
 */
@Entity
@Table(name = "user_alert_thresholds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAlertThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 传感器组ID（可空，为空表示大棚级别） */
    @Column(name = "group_id")
    private Long groupId;

    /** 传感器类型 */
    @Column(name = "sensor_type", nullable = false, length = 30)
    private String sensorType;

    /** 最低阈值 */
    @Column(name = "min_threshold")
    private Double minThreshold;

    /** 最高阈值 */
    @Column(name = "max_threshold")
    private Double maxThreshold;

    /** 是否启用 */
    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
