package com.greenhouse.app.data.model;

/**
 * 设备控制日志模型
 * <p>
 * 对应后端 GET /api/v1/control/logs 返回项。
 * source: MANUAL（手动）/ SCENE（场景触发）/ ALERT（预警联动）
 * </p>
 */
public class ControlLogItem {

    private Long id;
    private Long userId;
    private String username;
    private Long deviceId;
    private String deviceName;
    private String action;      // ON / OFF
    private String source;      // MANUAL / SCENE / ALERT
    private Long sceneId;
    private String sceneName;
    private Boolean success;
    private String failReason;
    private String createdAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public Long getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public String getAction() { return action; }
    public String getSource() { return source; }
    public Long getSceneId() { return sceneId; }
    public String getSceneName() { return sceneName; }
    public Boolean getSuccess() { return success; }
    public String getFailReason() { return failReason; }
    public String getCreatedAt() { return createdAt; }
}
