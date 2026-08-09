package com.greenhouse.app.data.model;

/**
 * 设备控制请求模型
 * <p>
 * 对应后端: POST /api/v1/control/actuator 请求体 {deviceId, action}
 * </p>
 */
public class ControlRequest {

    private Long deviceId;
    private String action;      // ON / OFF

    public ControlRequest() {}

    public ControlRequest(Long deviceId, String action) {
        this.deviceId = deviceId;
        this.action = action;
    }

    public Long getDeviceId() { return deviceId; }
    public String getAction() { return action; }

    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public void setAction(String action) { this.action = action; }
}