package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 传感器数据点
 */
public class SensorDataPoint {

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("deviceId")
    private long deviceId;

    @SerializedName("deviceName")
    private String deviceName;

    @SerializedName("sensorType")
    private String sensorType;

    private double value;

    @SerializedName("timestamp")
    private String timestamp;

    public long getGreenhouseId() { return greenhouseId; }
    public long getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public String getSensorType() { return sensorType; }
    public double getValue() { return value; }
    public String getTimestamp() { return timestamp; }
}
