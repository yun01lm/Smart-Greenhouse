package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

/**
 * 健康评分数据
 */
public class HealthScoreData {

    private long id;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("overallScore")
    private BigDecimal overallScore;

    private String level;
    private String levelColor;

    @SerializedName("envScore")
    private BigDecimal envScore;

    @SerializedName("visualScore")
    private BigDecimal visualScore;

    @SerializedName("weatherRisk")
    private String weatherRisk;

    @SerializedName("weatherFactor")
    private BigDecimal weatherFactor;

    private String recommendations;

    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public long getGreenhouseId() { return greenhouseId; }
    public BigDecimal getOverallScore() { return overallScore; }
    public String getLevel() { return level; }
    public String getLevelColor() { return levelColor; }
    public BigDecimal getEnvScore() { return envScore; }
    public BigDecimal getVisualScore() { return visualScore; }
    public String getWeatherRisk() { return weatherRisk; }
    public BigDecimal getWeatherFactor() { return weatherFactor; }
    public String getRecommendations() { return recommendations; }
    public String getCreatedAt() { return createdAt; }
}
