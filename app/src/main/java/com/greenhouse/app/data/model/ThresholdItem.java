package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 自定义预警阈值
 */
public class ThresholdItem {

    private Long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("greenhouseId")
    private Long greenhouseId;

    @SerializedName("groupId")
    private Long groupId;

    @SerializedName("sensorType")
    private String sensorType;

    @SerializedName("minThreshold")
    private Double minThreshold;

    @SerializedName("maxThreshold")
    private Double maxThreshold;

    private Boolean enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(Long greenhouseId) { this.greenhouseId = greenhouseId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }
    public Double getMinThreshold() { return minThreshold; }
    public void setMinThreshold(Double minThreshold) { this.minThreshold = minThreshold; }
    public Double getMaxThreshold() { return maxThreshold; }
    public void setMaxThreshold(Double maxThreshold) { this.maxThreshold = maxThreshold; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
