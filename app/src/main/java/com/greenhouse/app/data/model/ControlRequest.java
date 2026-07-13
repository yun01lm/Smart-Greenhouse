package com.greenhouse.app.data.model;

/**
 * 设备控制请求模型
 * <p>
 * 对应后端: POST /api/v1/control/actuator
 * </p>
 */
public class ControlRequest {

    private Long actuatorId;
    private String action;      // ON / OFF
    private Long greenhouseId;

    public ControlRequest(Long actuatorId, String action, Long greenhouseId) {
        this.actuatorId = actuatorId;
        this.action = action;
        this.greenhouseId = greenhouseId;
    }

    public Long getActuatorId() { return actuatorId; }
    public String getAction() { return action; }
    public Long getGreenhouseId() { return greenhouseId; }
}
