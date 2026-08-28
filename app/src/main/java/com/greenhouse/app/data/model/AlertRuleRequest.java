package com.greenhouse.app.data.model;

/**
 * 预警规则创建/更新请求（对齐后端 AlertRuleRequest）
 */
public class AlertRuleRequest {

    private long greenhouseId;
    private String sensorType;
    private String ruleType;
    private String conditionJson;
    private String alertLevel;
    private Long sceneId;
    private boolean enabled;

    public AlertRuleRequest(long greenhouseId, String sensorType, String ruleType,
                            String conditionJson, String alertLevel, Long sceneId, boolean enabled) {
        this.greenhouseId = greenhouseId;
        this.sensorType = sensorType;
        this.ruleType = ruleType;
        this.conditionJson = conditionJson;
        this.alertLevel = alertLevel;
        this.sceneId = sceneId;
        this.enabled = enabled;
    }

    public long getGreenhouseId() { return greenhouseId; }
    public String getSensorType() { return sensorType; }
    public String getRuleType() { return ruleType; }
    public String getConditionJson() { return conditionJson; }
    public String getAlertLevel() { return alertLevel; }
    public Long getSceneId() { return sceneId; }
    public boolean isEnabled() { return enabled; }
}
