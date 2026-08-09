package com.greenhouse.app.data.model;

/**
 * 历史数据查询请求模型
 * <p>
 * 对应后端: POST /api/v1/sensors/history 请求体。
 * startTime / endTime 为 epoch 毫秒；interval 支持 1m/5m/15m/1h/6h/1d。
 * </p>
 */
public class SensorHistoryRequest {

    private String sensorType;
    private Long startTime;
    private Long endTime;
    private String interval;

    public SensorHistoryRequest() {}

    public SensorHistoryRequest(String sensorType, Long startTime, Long endTime, String interval) {
        this.sensorType = sensorType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
    }

    public String getSensorType() { return sensorType; }
    public Long getStartTime() { return startTime; }
    public Long getEndTime() { return endTime; }
    public String getInterval() { return interval; }

    public void setSensorType(String sensorType) { this.sensorType = sensorType; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public void setInterval(String interval) { this.interval = interval; }
}