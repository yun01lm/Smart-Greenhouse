package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 病虫害诊断记录实体
 * <p>
 * 对应数据库第 12 号表 diagnostic_records。
 * 每次图片诊断自动保存，记录识别结果和用户信息。
 * </p>
 */
@Entity
@Table(name = "diagnostic_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 发起诊断的用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 关联的大棚ID */
    @Column(name = "greenhouse_id")
    private Long greenhouseId;

    /** 上传的图片路径 */
    @Column(name = "image_path", length = 500)
    private String imagePath;

    /** 识别出的病虫害名称 */
    @Column(name = "disease_name", length = 200)
    private String diseaseName;

    /** 识别置信度（0.0 ~ 1.0） */
    private Double confidence;

    /** 防治方案/建议 */
    @Column(columnDefinition = "TEXT")
    private String treatment;

    /** 识别引擎：baidu / resnet */
    @Column(name = "recognition_engine", length = 50)
    private String recognitionEngine;

    /** 是否已咨询专家 */
    @Column(name = "expert_consulted")
    @Builder.Default
    private Boolean expertConsulted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
