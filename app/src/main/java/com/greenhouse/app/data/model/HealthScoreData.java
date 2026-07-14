package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/**
 * 健康评分详情/报告模型
 * <p>
 * 对应后端 GET /api/v1/health/score 和 GET /api/v1/health/detail/{id} 的响应数据。
 * 包含综合评分、子维度评分、分析详情、改善建议等完整信息。
 * </p>
 */
public class HealthScoreData {

    private long id;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("greenhouseName")
    private String greenhouseName;

    @SerializedName("overallScore")
    private BigDecimal overallScore;

    private String level;

    @SerializedName("levelColor")
    private String levelColor;

    @SerializedName("envScore")
    private BigDecimal envScore;

    @SerializedName("visualScore")
    private BigDecimal visualScore;

    @SerializedName("weatherRisk")
    private String weatherRisk;

    @SerializedName("weatherFactor")
    private BigDecimal weatherFactor;

    @SerializedName("analysisJson")
    private AnalysisDetail analysisJson;

    private String recommendations;

    @SerializedName("createdAt")
    private String createdAt;

    // ===== Getter =====

    public long getId() { return id; }
    public long getGreenhouseId() { return greenhouseId; }
    public String getGreenhouseName() { return greenhouseName; }
    public BigDecimal getOverallScore() { return overallScore; }
    public String getLevel() { return level; }
    public String getLevelColor() { return levelColor; }
    public BigDecimal getEnvScore() { return envScore; }
    public BigDecimal getVisualScore() { return visualScore; }
    public String getWeatherRisk() { return weatherRisk; }
    public BigDecimal getWeatherFactor() { return weatherFactor; }
    public AnalysisDetail getAnalysisJson() { return analysisJson; }
    public String getRecommendations() { return recommendations; }
    public String getCreatedAt() { return createdAt; }

    // ===== 辅助方法 =====

    /**
     * 获取综合评分的整数显示
     */
    public int getOverallScoreInt() {
        return overallScore != null ? overallScore.intValue() : 0;
    }

    /**
     * 获取健康等级颜色（int值，用于 setTint）
     */
    public int getLevelColorInt() {
        if (levelColor == null) return 0xFF4CAF50;
        switch (levelColor) {
            case "blue": return 0xFF2196F3;
            case "yellow": return 0xFFFFC107;
            case "orange": return 0xFFFF9800;
            case "red": return 0xFFF44336;
            default: return 0xFF4CAF50;
        }
    }

    /**
     * 获取子维度评分格式化文本
     */
    public String getEnvScoreText() {
        return envScore != null ? String.valueOf(envScore.intValue()) : "--";
    }

    public String getVisualScoreText() {
        return visualScore != null ? String.valueOf(visualScore.intValue()) : "--";
    }

    /**
     * 是否有分析详情
     */
    public boolean hasAnalysisDetail() {
        return analysisJson != null;
    }

    /**
     * 是否有改善建议
     */
    public boolean hasRecommendations() {
        return recommendations != null && !recommendations.isEmpty();
    }

    // ===== 内部分析详情 =====

    /**
     * 分析详情（对应后端 analysisJson 字段）
     */
    public static class AnalysisDetail {

        @SerializedName("envDetail")
        private EnvDetail envDetail;

        @SerializedName("visualDetail")
        private VisualDetail visualDetail;

        @SerializedName("weatherImpact")
        private WeatherImpact weatherImpact;

        public EnvDetail getEnvDetail() { return envDetail; }
        public VisualDetail getVisualDetail() { return visualDetail; }
        public WeatherImpact getWeatherImpact() { return weatherImpact; }

        /**
         * 环境维度详情
         */
        public static class EnvDetail {
            @SerializedName("tempScore") private Integer tempScore;
            @SerializedName("tempComment") private String tempComment;
            @SerializedName("humidityScore") private Integer humidityScore;
            @SerializedName("humidityComment") private String humidityComment;
            @SerializedName("co2Score") private Integer co2Score;
            @SerializedName("co2Comment") private String co2Comment;
            @SerializedName("soilScore") private Integer soilScore;
            @SerializedName("soilComment") private String soilComment;

            public Integer getTempScore() { return tempScore; }
            public String getTempComment() { return tempComment; }
            public Integer getHumidityScore() { return humidityScore; }
            public String getHumidityComment() { return humidityComment; }
            public Integer getCo2Score() { return co2Score; }
            public String getCo2Comment() { return co2Comment; }
            public Integer getSoilScore() { return soilScore; }
            public String getSoilComment() { return soilComment; }
        }

        /**
         * 视觉维度详情
         */
        public static class VisualDetail {
            @SerializedName("leafHealth") private Integer leafHealth;
            @SerializedName("leafHealthComment") private String leafHealthComment;
            @SerializedName("growthRate") private Integer growthRate;
            @SerializedName("growthRateComment") private String growthRateComment;
            @SerializedName("diseaseRisk") private Integer diseaseRisk;
            @SerializedName("diseaseRiskComment") private String diseaseRiskComment;

            public Integer getLeafHealth() { return leafHealth; }
            public String getLeafHealthComment() { return leafHealthComment; }
            public Integer getGrowthRate() { return growthRate; }
            public String getGrowthRateComment() { return growthRateComment; }
            public Integer getDiseaseRisk() { return diseaseRisk; }
            public String getDiseaseRiskComment() { return diseaseRiskComment; }
        }

        /**
         * 天气影响
         */
        public static class WeatherImpact {
            @SerializedName("currentWeather") private String currentWeather;
            @SerializedName("forecast") private String forecast;
            @SerializedName("riskAssessment") private String riskAssessment;

            public String getCurrentWeather() { return currentWeather; }
            public String getForecast() { return forecast; }
            public String getRiskAssessment() { return riskAssessment; }
        }
    }
}
