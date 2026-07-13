package com.greenhouse.module.diagnosis.dto;

import com.greenhouse.entity.DiagnosticRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 诊断结果响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisResponse {

    private Long id;
    private Long userId;
    private Long greenhouseId;
    private String imagePath;
    private String diseaseName;
    private Double confidence;
    private String treatment;
    private String recognitionEngine;
    private Boolean expertConsulted;
    private Boolean needExpert;
    private LocalDateTime createdAt;

    public static DiagnosisResponse fromEntity(DiagnosticRecord record) {
        return DiagnosisResponse.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .greenhouseId(record.getGreenhouseId())
                .imagePath(record.getImagePath())
                .diseaseName(record.getDiseaseName())
                .confidence(record.getConfidence())
                .treatment(record.getTreatment())
                .recognitionEngine(record.getRecognitionEngine())
                .expertConsulted(record.getExpertConsulted())
                .needExpert(record.getConfidence() != null && record.getConfidence() < 0.70)
                .createdAt(record.getCreatedAt())
                .build();
    }
}
