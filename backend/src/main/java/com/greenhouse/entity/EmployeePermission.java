package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 员工权限实体
 * <p>
 * 对应数据库第 8 号表 employee_permissions。
 * 棚主为每个员工分配具体的大棚访问权限和功能权限。
 * 一个员工可以对多个大棚拥有不同的权限。
 * </p>
 */
@Entity
@Table(name = "employee_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 员工ID（FK → users.id，role=WORKER） */
    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    /** 棚主ID（FK → users.id，role=OWNER） */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 授权的大棚ID（FK → greenhouses.id） */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    // ===== 功能权限（6个布尔字段） =====

    /** 查看数据权限（传感器数据、图表、看板） */
    @Column(name = "can_view_data", nullable = false)
    @Builder.Default
    private Boolean canViewData = false;

    /** 控制设备权限（水泵、风机、遮阳帘等） */
    @Column(name = "can_control_device", nullable = false)
    @Builder.Default
    private Boolean canControlDevice = false;

    /** 病虫害诊断权限（拍照识别） */
    @Column(name = "can_diagnose", nullable = false)
    @Builder.Default
    private Boolean canDiagnose = false;

    /** 专家咨询权限（发起咨询对话） */
    @Column(name = "can_ask_expert", nullable = false)
    @Builder.Default
    private Boolean canAskExpert = false;

    /** 查看预警权限 */
    @Column(name = "can_view_alerts", nullable = false)
    @Builder.Default
    private Boolean canViewAlerts = false;

    /** 查看历史数据权限 */
    @Column(name = "can_view_history", nullable = false)
    @Builder.Default
    private Boolean canViewHistory = false;

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
