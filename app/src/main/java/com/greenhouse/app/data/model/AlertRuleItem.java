package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 预警规则（对应后端 /api/v1/alerts/rules）
 */
public class AlertRuleItem {

    private long id;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("sensorType")
    private String sensorType;

    @SerializedName("ruleType")
    private String ruleType;

    @SerializedName("conditionJson")
    private String conditionJson;

    @SerializedName("alertLevel")
    private String alertLevel;

    @SerializedName("sceneId")
    private Long sceneId;

    private boolean enabled;

    @SerializedName("createdAt")
    private String createdAt;

    // 展示辅助：解析 conditionJson 的 min/max
    public Double getMin() {
        try {
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(conditionJson).getAsJsonObject();
            return o.has("min") ? o.get("min").getAsDouble() : null;
        } catch (Exception e) { return null; }
    }

    public Double getMax() {
        try {
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(conditionJson).getAsJsonObject();
            return o.has("max") ? o.get("max").getAsDouble() : null;
        } catch (Exception e) { return null; }
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(long greenhouseId) { this.greenhouseId = greenhouseId; }
    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getConditionJson() { return conditionJson; }
    public void setConditionJson(String conditionJson) { this.conditionJson = conditionJson; }
    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
    public Long getSceneId() { return sceneId; }
    public void setSceneId(Long sceneId) { this.sceneId = sceneId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
