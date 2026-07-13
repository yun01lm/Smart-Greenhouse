package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 预警记录
 */
public class AlertItem {

    private long id;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    private String level;       // INFO / WARNING / CRITICAL
    private String title;
    private String content;

    @SerializedName("sensorType")
    private String sensorType;

    @SerializedName("sensorValue")
    private Double sensorValue;

    @SerializedName("readStatus")
    private boolean readStatus;

    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public long getGreenhouseId() { return greenhouseId; }
    public String getLevel() { return level; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getSensorType() { return sensorType; }
    public Double getSensorValue() { return sensorValue; }
    public boolean isReadStatus() { return readStatus; }
    public String getCreatedAt() { return createdAt; }
}
