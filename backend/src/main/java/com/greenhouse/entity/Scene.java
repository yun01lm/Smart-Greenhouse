package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 场景联动实体
 * <p>
 * 对应数据库第 11 号表 scenes。
 * 场景包含一组预定义的动作，用户可手动一键执行。
 * Phase 1 只做手动执行，自动触发条件留给 Phase 2 预警引擎（C6）。
 * </p>
 */
@Entity
@Table(name = "scenes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 场景名称（如"夏季降温"、"夜间灌溉"） */
    @Column(nullable = false, length = 100)
    private String name;

    /** 场景描述 */
    @Column(length = 500)
    private String description;

    /**
     * 触发条件 JSON（Phase 2 使用，Phase 1 可为空）
     * 示例：{"sensorType":"TEMPERATURE","operator":"gt","value":35}
     */
    @Column(name = "trigger_condition", columnDefinition = "JSON")
    private String triggerCondition;

    /**
     * 执行动作列表 JSON（必填）
     * 示例：[{"deviceId":1,"action":"ON"},{"deviceId":2,"action":"OFF"}]
     */
    @Column(name = "actions_json", columnDefinition = "JSON", nullable = false)
    private String actionsJson;

    /** 所属大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

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
