package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备分组实体
 * <p>
 * 将同一大棚下的多个设备归为一组，便于批量控制和数据查看。
 * 例如："环境传感器组"包含温度、湿度、CO2传感器；"灌溉组"包含水泵和土壤湿度传感器。
 * </p>
 */
@Entity
@Table(name = "device_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 分组名称（同一大棚下唯一） */
    @Column(nullable = false, length = 100)
    private String name;

    /** 所属大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 分组描述 */
    @Column(length = 500)
    private String description;

    /**
     * 组内设备ID列表
     * <p>
     * 使用 @ElementCollection 存储为独立的关联表 device_group_members。
     * 不使用 @ManyToMany 是因为设备可能同时属于多个分组，
     * 且我们只需要存储设备ID而非完整的 Device 对象引用。
     * </p>
     */
    @ElementCollection
    @CollectionTable(
            name = "device_group_members",
            joinColumns = @JoinColumn(name = "group_id")
    )
    @Column(name = "device_id")
    @Builder.Default
    private List<Long> deviceIds = new ArrayList<>();

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
