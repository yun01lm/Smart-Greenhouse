package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户实体
 * <p>
 * 四种角色：
 * ADMIN(管理员) — 仅Web端，管理平台全部数据
 * OWNER(棚主) — APP+Web端，管理自己的大棚和员工
 * WORKER(员工) — 仅APP端，归属一个棚主，权限由棚主分配
 * EXPERT(专家) — 仅Web端，接收用户求助，7天授权查看数据
 * </p>
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名（登录用，唯一） */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** 密码（BCrypt加密） */
    @Column(nullable = false, length = 200)
    private String password;

    /** 手机号（唯一） */
    @Column(unique = true, length = 20)
    private String phone;

    /** 真实姓名 */
    @Column(length = 50)
    private String realName;

    /** 角色 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // ===== 员工专属字段 =====

    /** 归属棚主ID（仅WORKER角色使用，员工单归属） */
    @Column(name = "owner_id")
    private Long ownerId;

    // ===== 专家专属字段 =====

    /** 专家专业领域 */
    @Column(length = 200)
    private String expertSpecialty;

    /** 专家在线状态（仅EXPERT角色使用） */
    @Column(name = "expert_status")
    @Enumerated(EnumType.STRING)
    private ExpertStatus expertStatus;

    // ===== 通用字段 =====

    /** 账号状态：true=启用，false=禁用 */
    @Builder.Default
    @Column(nullable = false)
    private Boolean status = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 登录失败计数（B4安全加固）
    @Column(name = "login_fail_count")
    @Builder.Default
    private Integer loginFailCount = 0;

    // 锁定截止时间（B4安全加固）
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

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

    // ===== 角色枚举 =====

    public enum Role {
        ADMIN,   // 管理员（仅Web端）
        OWNER,   // 棚主（APP+Web端）
        WORKER,  // 员工（仅APP端）
        EXPERT   // 专家（仅Web端）
    }

    public enum ExpertStatus {
        ONLINE,   // 在线
        OFFLINE,  // 离线
        BUSY      // 忙碌
    }
}
