package com.greenhouse.app.data.model;

import java.util.List;

/**
 * 创建场景请求模型
 * <p>对应 POST /api/v1/control/scenes，body: {name, description, actions:[{deviceId, action}]}</p>
 */
public class CreateSceneRequest {

    private String name;
    private String description;
    private List<SceneActionItem> actions;

    public CreateSceneRequest(String name, String description, List<SceneActionItem> actions) {
        this.name = name;
        this.description = description;
        this.actions = actions;
    }

    /** 单个设备动作 */
    public static class SceneActionItem {
        private long deviceId;
        private String action; // ON / OFF

        public SceneActionItem(long deviceId, String action) {
            this.deviceId = deviceId;
            this.action = action;
        }

        public long getDeviceId() { return deviceId; }
        public String getAction() { return action; }
    }
}