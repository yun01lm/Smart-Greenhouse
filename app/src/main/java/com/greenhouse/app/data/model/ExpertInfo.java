package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 专家信息模型
 * <p>
 * 对应 GET /api/v1/experts 响应。
 * </p>
 */
public class ExpertInfo {

    private long id;

    @SerializedName("realName")
    private String realName;

    @SerializedName("expertSpecialty")
    private String expertSpecialty;

    @SerializedName("expertStatus")
    private int expertStatus; // 1=在线, 0=离线

    private double rating;

    @SerializedName("consultCount")
    private int consultCount;

    @SerializedName("currentCount")
    private int currentCount;

    private String introduction;

    // ===== Getter =====

    public long getId() { return id; }
    public String getRealName() { return realName; }
    public String getExpertSpecialty() { return expertSpecialty; }
    public int getExpertStatus() { return expertStatus; }
    public double getRating() { return rating; }
    public int getConsultCount() { return consultCount; }
    public int getCurrentCount() { return currentCount; }
    public String getIntroduction() { return introduction; }

    // ===== 辅助方法 =====

    public boolean isOnline() {
        return expertStatus == 1;
    }

    public String getStatusText() {
        return expertStatus == 1 ? "在线" : "离线";
    }

    public String getRatingText() {
        return String.format("%.1f", rating);
    }

    public String getConsultCountText() {
        return "已咨询 " + consultCount + " 次";
    }
}
