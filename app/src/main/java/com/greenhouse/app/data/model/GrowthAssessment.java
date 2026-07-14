package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 作物长势评估结果
 * <p>
 * 对应后端 GET /api/v1/growth/latest 和 GET /api/v1/growth/history 的响应数据。
 * 包含截帧图片、AI识别的生长阶段、株高/叶面积/叶色等长势特征。
 * </p>
 */
public class GrowthAssessment {

    private long id;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("cropCycleId")
    private long cropCycleId;

    @SerializedName("imagePath")
    private String imagePath;

    @SerializedName("growthStage")
    private String growthStage;

    @SerializedName("plantHeight")
    private double plantHeight;

    @SerializedName("leafArea")
    private double leafArea;

    @SerializedName("leafColor")
    private String leafColor;

    @SerializedName("healthScore")
    private double healthScore;

    @SerializedName("createdAt")
    private String createdAt;

    // ===== Getter =====

    public long getId() { return id; }
    public long getGreenhouseId() { return greenhouseId; }
    public long getCropCycleId() { return cropCycleId; }
    public String getImagePath() { return imagePath; }
    public String getGrowthStage() { return growthStage; }
    public double getPlantHeight() { return plantHeight; }
    public double getLeafArea() { return leafArea; }
    public String getLeafColor() { return leafColor; }
    public double getHealthScore() { return healthScore; }
    public String getCreatedAt() { return createdAt; }

    // ===== 辅助方法 =====

    /**
     * 获取健康评分百分比文本
     */
    public String getHealthScoreText() {
        return String.format("%.0f", healthScore);
    }

    /**
     * 获取株高显示文本（cm）
     */
    public String getPlantHeightText() {
        return String.format("%.1f cm", plantHeight);
    }

    /**
     * 获取叶面积显示文本（cm²）
     */
    public String getLeafAreaText() {
        return String.format("%.1f cm²", leafArea);
    }

    /**
     * 获取叶色描述
     */
    public String getLeafColorText() {
        if (leafColor == null || leafColor.isEmpty()) return "--";
        return leafColor;
    }

    /**
     * 获取生长阶段描述
     */
    public String getGrowthStageText() {
        if (growthStage == null || growthStage.isEmpty()) return "未知";
        return growthStage;
    }

    /**
     * 健康评分等级（绿≥80 / 蓝60-80 / 黄40-60 / 橙20-40 / 红<20）
     */
    public String getHealthLevel() {
        if (healthScore >= 80) return "健康";
        if (healthScore >= 60) return "良好";
        if (healthScore >= 40) return "关注";
        if (healthScore >= 20) return "警告";
        return "危险";
    }

    /**
     * 健康评分等级颜色（Android Color int）
     */
    public int getHealthLevelColor() {
        if (healthScore >= 80) return 0xFF4CAF50; // 绿色
        if (healthScore >= 60) return 0xFF2196F3; // 蓝色
        if (healthScore >= 40) return 0xFFFFC107; // 黄色
        if (healthScore >= 20) return 0xFFFF9800; // 橙色
        return 0xFFF44336; // 红色
    }

    /**
     * 是否有图片
     */
    public boolean hasImage() {
        return imagePath != null && !imagePath.isEmpty();
    }
}
