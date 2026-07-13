package com.greenhouse.app.data.model;

/**
 * 场景执行请求模型
 * <p>
 * 对应后端: POST /api/v1/control/scenes/{id}/execute
 * </p>
 */
public class SceneExecuteRequest {

    private Long greenhouseId;

    public SceneExecuteRequest(Long greenhouseId) {
        this.greenhouseId = greenhouseId;
    }

    public Long getGreenhouseId() { return greenhouseId; }
}
