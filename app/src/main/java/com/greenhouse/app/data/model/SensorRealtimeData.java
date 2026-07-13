package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * 传感器实时数据（响应模型）
 */
public class SensorRealtimeData {

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("greenhouseName")
    private String greenhouseName;

    @SerializedName("dataByType")
    private Map<String, List<SensorDataPoint>> dataByType;

    public long getGreenhouseId() { return greenhouseId; }
    public String getGreenhouseName() { return greenhouseName; }
    public Map<String, List<SensorDataPoint>> getDataByType() { return dataByType; }
}
