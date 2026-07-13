package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 大棚实体
 * <p>
 * 每个大棚归属一个棚主(OWNER)，包含五级地区地址。
 * 管理员可查看全部大棚，棚主只看自己的，员工只看被授权的。
 * </p>
 */
@Entity
@Table(name = "greenhouses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Greenhouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 大棚名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 位置描述（如"村东头"） */
    @Column(length = 200)
    private String location;

    /** 当前种植作物类型（如"黄瓜"、"番茄"） */
    @Column(name = "crop_type", length = 50)
    private String cropType;

    /** 归属棚主ID */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // ===== 五级地区地址 =====

    @Column(length = 50)
    private String province;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String district;

    @Column(length = 50)
    private String town;

    @Column(length = 50)
    private String village;

    /** 大棚状态：true=正常，false=停用 */
    @Builder.Default
    @Column(nullable = false)
    private Boolean status = true;

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
