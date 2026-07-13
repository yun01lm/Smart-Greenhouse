package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 设备实体
 * <p>
 * 代表大棚内的物联网设备（传感器、控制器等）。
 * 每个设备属于一个特定的大棚，有唯一的 MQTT topic 用于数据传输。
 * </p>
 */
@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 设备名称（如"1号温度传感器"、"水泵控制器"） */
    @Column(nullable = false, length = 100)
    private String name;

    /** 设备编号（用户自定义，同一大棚下唯一，如"TEMP-001"） */
    @Column(nullable = false, length = 50)
    private String deviceSn;

    /**
     * 设备类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    /**
     * 传感器子类型（仅传感器类设备使用，控制器类为 null）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", length = 30)
    private SensorType sensorType;

    /** 设备状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.OFFLINE;

    /** 所属大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 上次数据上报时间 */
    @Column(name = "last_data_time")
    private LocalDateTime lastDataTime;

    /** 上次数值（传感器记录最新读数，控制器记录最新开关状态） */
    @Column(name = "last_value", length = 100)
    private String lastValue;

    /** MQTT 主题（自动生成：greenhouse/{greenhouseId}/device/{deviceSn}） */
    @Column(name = "mqtt_topic", length = 200)
    private String mqttTopic;

    /** 设备描述 */
    @Column(length = 500)
    private String description;

    /** 安装位置（如"大棚东北角"） */
    @Column(length = 200)
    private String installLocation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== 枚举定义 =====

    /** 设备类型 */
    public enum DeviceType {
        /** 传感器（温度/湿度/光照/CO2/土壤等） */
        SENSOR,
        /** 控制器（水泵/风机/遮阳帘/补光灯等） */
        CONTROLLER
    }

    /** 传感器子类型 */
    public enum SensorType {
        TEMPERATURE("温度"),
        HUMIDITY("湿度"),
        LIGHT("光照"),
        CO2("CO2浓度"),
        SOIL_MOISTURE("土壤湿度"),
        SOIL_TEMP("土壤温度"),
        SOIL_PH("土壤pH"),
        WIND_SPEED("风速");

        private final String label;

        SensorType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** 设备状态 */
    public enum DeviceStatus {
        /** 在线（有数据上报） */
        ONLINE,
        /** 离线（长时间无数据） */
        OFFLINE,
        /** 告警（数据异常） */
        ALARM
    }

    // ===== 生命周期 =====

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // 自动生成 MQTT topic
        if (mqttTopic == null) {
            mqttTopic = "greenhouse/" + greenhouseId + "/device/" + deviceSn;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
