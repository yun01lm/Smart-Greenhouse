package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 诊断结果响应模型
 * <p>
 * 对应后端: POST /api/v1/diagnosis/recognize
 * </p>
 */
public class DiagnosisResponse {

    private Long id;
    private Long userId;
    private Long greenhouseId;
    private String imagePath;
    private String diseaseName;
    private Double confidence;
    private String treatment;
    private String recognitionEngine;

    @SerializedName("expertConsulted")
    private Boolean expertConsulted;

    @SerializedName("needExpert")
    private Boolean needExpert;

    private String createdAt;

    // ===== Getter =====

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getGreenhouseId() { return greenhouseId; }
    public String getImagePath() { return imagePath; }
    public String getDiseaseName() { return diseaseName; }
    public Double getConfidence() { return confidence; }
    public String getTreatment() { return treatment; }
    public String getRecognitionEngine() { return recognitionEngine; }
    public Boolean getExpertConsulted() { return expertConsulted; }
    public Boolean getNeedExpert() { return needExpert; }
    public String getCreatedAt() { return createdAt; }

    /**
     * 置信度百分比文本
     */
    public String getConfidenceText() {
        if (confidence == null) return "未知";
        return String.format("%.0f%%", confidence * 100);
    }

    /**
     * 置信度颜色等级
     * @return 0=绿色(>=80%), 1=黄色(70-80%), 2=红色(<70%)
     */
    public int getConfidenceLevel() {
        if (confidence == null) return 2;
        if (confidence >= 0.80) return 0;
        if (confidence >= 0.70) return 1;
        return 2;
    }
}
