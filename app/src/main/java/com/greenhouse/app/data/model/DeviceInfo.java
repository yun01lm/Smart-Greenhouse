package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 设备信息模型
 * <p>
 * 对应后端: GET /api/v1/greenhouses/{greenhouseId}/devices → DeviceResponse
 * </p>
 */
public class DeviceInfo {

    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("deviceSn")
    private String deviceSn;

    /** SENSOR / CONTROLLER */
    @SerializedName("deviceType")
    private String deviceType;

    /** 传感器子类型（仅 SENSOR 设备有值） */
    @SerializedName("sensorType")
    private String sensorType;

    /** ONLINE / OFFLINE / ALARM */
    @SerializedName("status")
    private String status;

    @SerializedName("greenhouseId")
    private Long greenhouseId;

    /** 控制器记录最近开关状态 ON/OFF */
    @SerializedName("lastValue")
    private String lastValue;

    @SerializedName("lastDataTime")
    private String lastDataTime;

    @SerializedName("mqttTopic")
    private String mqttTopic;

    /** 安装位置（如"大棚东北角"） */
    @SerializedName("installLocation")
    private String installLocation;

    @SerializedName("description")
    private String description;

    @SerializedName("createdAt")
    private String createdAt;

    public DeviceInfo() {}

    // ===== Getter =====

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDeviceSn() { return deviceSn; }
    public String getDeviceType() { return deviceType; }
    public String getSensorType() { return sensorType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getGreenhouseId() { return greenhouseId; }
    public String getLastValue() { return lastValue; }
    public void setLastValue(String lastValue) { this.lastValue = lastValue; }
    public String getLastDataTime() { return lastDataTime; }
    public String getMqttTopic() { return mqttTopic; }
    public String getInstallLocation() { return installLocation; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }

    // ===== 状态辅助 =====

    public boolean isController() { return "CONTROLLER".equals(deviceType); }

    /** 是否在线（仅 OFFLINE 视为离线） */
    public boolean isOnline() { return !"OFFLINE".equals(status); }

    /** 控制器是否处于开启状态 */
    public boolean isRunning() { return "ON".equals(lastValue); }

    /** 设备类型中文名 */
    public String getDeviceTypeName() {
        return isController() ? "控制器" : "传感器";
    }
}
