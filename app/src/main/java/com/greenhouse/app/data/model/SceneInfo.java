package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 场景联动信息模型
 * <p>
 * 对应后端: GET /api/v1/control/scenes
 * </p>
 */
public class SceneInfo {

    private Long id;
    private String name;                    // 场景名称（如"高温通风"）
    private String description;             // 场景描述
    private Boolean enabled;
    private List<SceneAction> actionsJson;  // 场景包含的设备操作列表
    private Long greenhouseId;
    private String createdAt;

    public SceneInfo() {}

    // ===== Getter =====

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Boolean isEnabled() { return enabled != null && enabled; }
    public List<SceneAction> getActionsJson() { return actionsJson; }
    public Long getGreenhouseId() { return greenhouseId; }
    public String getCreatedAt() { return createdAt; }

    /**
     * 生成场景操作摘要（如"开启通风风机 + 展开遮阳网"）
     */
    public String getActionsSummary() {
        if (actionsJson == null || actionsJson.isEmpty()) return "无操作";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(actionsJson.size(), 3); i++) {
            if (i > 0) sb.append(" + ");
            sb.append(actionsJson.get(i).getDescription());
        }
        if (actionsJson.size() > 3) sb.append(" ...");
        return sb.toString();
    }

    /**
     * 场景中单个操作
     */
    public static class SceneAction {
        private Long actuatorId;
        private String action;      // ON / OFF / SET
        private String description; // 如"开启通风风机"

        public SceneAction() {}

        public Long getActuatorId() { return actuatorId; }
        public String getAction() { return action; }
        public String getDescription() { return description; }
    }
}
