package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 设备控制日志实体
 * <p>
 * 对应数据库第 14 号表 control_logs。
 * 每次设备控制操作自动记录，用于审计和追溯。
 * </p>
 */
@Entity
@Table(name = "control_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作人ID（可空，系统自动触发时为空） */
    @Column(name = "user_id")
    private Long userId;

    /** 被控制的设备ID */
    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    /** 控制动作：ON / OFF */
    @Column(nullable = false, length = 10)
    private String action;

    /**
     * 操作来源：MANUAL（手动）/ SCENE（场景联动）/ ALERT（预警触发，Phase 2）
     */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String source = "MANUAL";

    /** 场景ID（场景触发时记录） */
    @Column(name = "scene_id")
    private Long sceneId;

    /** 控制是否成功 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean success = true;

    /** 失败原因 */
    @Column(length = 500)
    private String failReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
