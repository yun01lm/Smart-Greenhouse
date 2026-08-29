package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 固件实体（硬件档案）
 * <p>
 * 出厂预注册的硬件身份。每片 ESP32 出厂时由管理员批量预注册一个固件ID
 * （8位数字，全局唯一），写死进固件并印在标签上。
 * 用户绑定时将固件绑定到自家大棚的设备，生成业务 SN。
 * </p>
 *
 * <h3>固件状态</h3>
 * <ul>
 *   <li>UNBOUND：已预注册，尚未被用户绑定</li>
 *   <li>BOUND：已被绑定到某台设备（boundDeviceId 指向 devices.id）</li>
 * </ul>
 */
@Entity
@Table(name = "firmwares")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Firmware {

    @Id
    @Column(name = "firmware_id", nullable = false, length = 8)
    private String firmwareId;

    /** 设备类型（传感器/控制器），与固件硬件能力一致 */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private Device.DeviceType deviceType;

    /** 传感器子类型（仅传感器类固件，控制器类为 null） */
    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", length = 30)
    private Device.SensorType sensorType;

    /** 固件版本号，如 "1.0.0" */
    @Column(name = "firmware_version", length = 20)
    private String firmwareVersion;

    /** 出厂批次号，如 "B20260801" */
    @Column(name = "batch_no", length = 30)
    private String batchNo;

    /** 固件状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Status status = Status.UNBOUND;

    /** 绑定设备ID（绑定后回填 devices.id） */
    @Column(name = "bound_device_id")
    private Long boundDeviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 固件状态枚举 */
    public enum Status {
        /** 已预注册，未绑定 */
        UNBOUND,
        /** 已绑定设备 */
        BOUND
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
