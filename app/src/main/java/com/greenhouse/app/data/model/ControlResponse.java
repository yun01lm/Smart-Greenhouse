package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 设备控制响应模型
 * <p>
 * 对应后端: POST /api/v1/control/actuator 和 POST /api/v1/control/scenes/{id}/execute
 * </p>
 */
public class ControlResponse {

    // 单设备控制响应字段
    private Long actuatorId;
    private String actuatorName;
    private String action;
    private String previousState;
    private String currentState;
    private Long deviceId;
    private Long groupId;
    private String zoneLabel;
    private String executedAt;

    // 场景执行响应字段
    private Long sceneId;
    private String sceneName;
    private List<ActionResult> results;

    public ControlResponse() {}

    // ===== Getter =====

    public Long getActuatorId() { return actuatorId; }
    public String getActuatorName() { return actuatorName; }
    public String getAction() { return action; }
    public String getPreviousState() { return previousState; }
    public String getCurrentState() { return currentState; }
    public Long getDeviceId() { return deviceId; }
    public Long getGroupId() { return groupId; }
    public String getZoneLabel() { return zoneLabel; }
    public String getExecutedAt() { return executedAt; }

    public Long getSceneId() { return sceneId; }
    public String getSceneName() { return sceneName; }
    public List<ActionResult> getResults() { return results; }

    /**
     * 是否为场景执行响应
     */
    public boolean isSceneResult() {
        return sceneId != null;
    }

    /**
     * 生成结果描述
     */
    public String getResultText() {
        if (isSceneResult()) {
            return "场景「" + sceneName + "」已执行";
        }
        return actuatorName + " 已" + ("ON".equals(action) ? "开启" : "关闭");
    }

    /**
     * 单个操作结果
     */
    public static class ActionResult {
        private Long actuatorId;
        private String actuatorName;
        private String action;
        private String status;  // SUCCESS / FAILED

        public ActionResult() {}

        public Long getActuatorId() { return actuatorId; }
        public String getActuatorName() { return actuatorName; }
        public String getAction() { return action; }
        public String getStatus() { return status; }

        public boolean isSuccess() { return "SUCCESS".equals(status); }
    }
}
