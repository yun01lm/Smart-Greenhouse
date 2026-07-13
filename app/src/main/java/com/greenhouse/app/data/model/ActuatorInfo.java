package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 执行器/设备信息模型
 * <p>
 * 对应后端: GET /api/v1/devices (设备列表中的执行器)
 * </p>
 */
public class ActuatorInfo {

    private Long id;
    private String name;            // 设备名称（如"通风风机"）
    private String type;            // 设备类型: FAN / ROLLER / SHADE / VALVE / LIGHT
    private String status;          // 状态: ON / OFF / RUNNING
    private Boolean online;         // 是否在线
    private Long deviceId;          // ESP32 设备ID
    private Long groupId;           // 传感器组ID
    private String zoneLabel;       // 区域标注（如"东侧"）
    private String lastHeartbeat;   // 最后心跳时间

    public ActuatorInfo() {}

    // ===== Getter =====

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public Boolean isOnline() { return online != null && online; }
    public Long getDeviceId() { return deviceId; }
    public Long getGroupId() { return groupId; }
    public String getZoneLabel() { return zoneLabel; }
    public String getLastHeartbeat() { return lastHeartbeat; }

    // ===== 状态辅助 =====

    public boolean isRunning() {
        return "ON".equals(status) || "RUNNING".equals(status);
    }

    /**
     * 设备类型中文名
     */
    public String getTypeName() {
        if (type == null) return "未知";
        switch (type) {
            case "FAN": return "通风风机";
            case "ROLLER": return "电动卷帘";
            case "SHADE": return "遮阳网";
            case "VALVE": return "滴灌阀门";
            case "LIGHT": return "补光灯";
            default: return type;
        }
    }

    /**
     * 设备类型图标资源名
     */
    public String getTypeIconName() {
        if (type == null) return "ic_device_default";
        switch (type) {
            case "FAN": return "ic_device_fan";
            case "ROLLER": return "ic_device_roller";
            case "SHADE": return "ic_device_shade";
            case "VALVE": return "ic_device_valve";
            case "LIGHT": return "ic_device_light";
            default: return "ic_device_default";
        }
    }

    /**
     * 设备类型图标 drawable res ID（由 Adapter 设置）
     */
    public int getTypeIconRes() {
        switch (type != null ? type : "") {
            case "FAN": return com.greenhouse.app.R.drawable.ic_device_fan;
            case "ROLLER": return com.greenhouse.app.R.drawable.ic_device_roller;
            case "SHADE": return com.greenhouse.app.R.drawable.ic_device_shade;
            case "VALVE": return com.greenhouse.app.R.drawable.ic_device_valve;
            case "LIGHT": return com.greenhouse.app.R.drawable.ic_device_light;
            default: return com.greenhouse.app.R.drawable.ic_device_default;
        }
    }
}
