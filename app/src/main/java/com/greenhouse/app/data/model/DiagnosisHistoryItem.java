package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 诊断历史列表项模型
 * <p>
 * 对应后端: GET /api/v1/diagnosis/records
 * </p>
 */
public class DiagnosisHistoryItem {

    private Long id;
    private String imagePath;
    private String diseaseName;
    private Double confidence;
    private String recognitionEngine;

    @SerializedName("expertConsulted")
    private Boolean expertConsulted;

    @SerializedName("needExpert")
    private Boolean needExpert;

    private String createdAt;

    // 从详情传递的额外字段
    private String treatment;

    public DiagnosisHistoryItem() {
    }

    public Long getId() { return id; }
    public String getImagePath() { return imagePath; }
    public String getDiseaseName() { return diseaseName; }
    public Double getConfidence() { return confidence; }
    public String getRecognitionEngine() { return recognitionEngine; }
    public Boolean getExpertConsulted() { return expertConsulted; }
    public Boolean getNeedExpert() { return needExpert; }
    public String getCreatedAt() { return createdAt; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }

    public String getConfidenceText() {
        if (confidence == null) return "未知";
        return String.format("%.0f%%", confidence * 100);
    }

    public int getConfidenceLevel() {
        if (confidence == null) return 2;
        if (confidence >= 0.80) return 0;
        if (confidence >= 0.70) return 1;
        return 2;
    }

    /**
     * 从 DiagnosisResponse 转换（含 treatment）
     */
    public static DiagnosisHistoryItem fromResponse(DiagnosisResponse response) {
        DiagnosisHistoryItem item = new DiagnosisHistoryItem();
        item.id = response.getId();
        item.imagePath = response.getImagePath();
        item.diseaseName = response.getDiseaseName();
        item.confidence = response.getConfidence();
        item.recognitionEngine = response.getRecognitionEngine();
        item.expertConsulted = response.getExpertConsulted();
        item.needExpert = response.getNeedExpert();
        item.createdAt = response.getCreatedAt();
        item.treatment = response.getTreatment();
        return item;
    }
}
