package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 设备控制结果模型
 * <p>
 * 对应后端: ControlLogResponse（单设备控制与场景执行均返回该结构）
 * </p>
 */
public class DeviceControlResult {

    @SerializedName("id")
    private Long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("username")
    private String username;

    @SerializedName("deviceId")
    private Long deviceId;

    @SerializedName("deviceName")
    private String deviceName;

    @SerializedName("action")
    private String action;

    @SerializedName("source")
    private String source;

    @SerializedName("sceneId")
    private Long sceneId;

    @SerializedName("success")
    private Boolean success;

    @SerializedName("failReason")
    private String failReason;

    @SerializedName("createdAt")
    private String createdAt;

    public DeviceControlResult() {}

    // ===== Getter =====

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public Long getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public String getAction() { return action; }
    public String getSource() { return source; }
    public Long getSceneId() { return sceneId; }
    public Boolean getSuccess() { return success; }
    public String getFailReason() { return failReason; }
    public String getCreatedAt() { return createdAt; }

    /** 是否执行成功 */
    public boolean isSuccess() { return success != null && success; }

    /** 结果描述（如"通风风机 已开启"） */
    public String getResultText() {
        String name = deviceName != null ? deviceName : ("设备#" + deviceId);
        String act = "ON".equals(action) ? "开启" : "关闭";
        return name + " " + (isSuccess() ? ("已" + act) : ("操作失败" + (failReason != null ? "：" + failReason : "")));
    }
}