package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 传感器日汇总实体
 * <p>
 * 每日由定时任务从 InfluxDB 原始数据聚合生成（日均/最小/最大/条数），
 * 供 7 天/30 天历史趋势图直接读取，避免每次实时扫描 InfluxDB 原始数据。
 * 写入后为追加式数据（只写一次不改），幂等：同一天已存在则跳过。
 * </p>
 */
@Entity
@Table(name = "sensor_daily_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gh_dev_type_date",
                columnNames = {"greenhouse_id", "device_id", "sensor_type", "stat_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 大棚ID */
    @Column(name = "greenhouse_id", nullable = false)
    private Long greenhouseId;

    /** 设备ID（按设备维度聚合，读取时可再汇总为大棚日均） */
    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    /** 传感器类型（TEMPERATURE/HUMIDITY/CO2/LIGHT/SOIL_TEMP/SOIL_MOISTURE/SOIL_PH/WIND_SPEED） */
    @Column(name = "sensor_type", nullable = false, length = 30)
    private String sensorType;

    /** 统计日期（Asia/Shanghai 时区） */
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    /** 日均值 */
    @Column(name = "avg_value")
    private Double avgValue;

    /** 当日最小值 */
    @Column(name = "min_value")
    private Double minValue;

    /** 当日最大值 */
    @Column(name = "max_value")
    private Double maxValue;

    /** 当日数据条数 */
    @Column(name = "data_count")
    private Long dataCount;

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