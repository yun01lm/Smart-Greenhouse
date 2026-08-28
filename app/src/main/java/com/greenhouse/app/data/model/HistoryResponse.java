package com.greenhouse.app.data.model;

import java.util.List;

/**
 * 历史数据查询响应模型
 * <p>
 * 对应后端: GET /api/v1/sensors/history
 * </p>
 */
public class HistoryResponse {

    private Long greenhouseId;
    private Long groupId;
    private String sensorType;
    private String aggregation;
    private String unit;
    private List<HistoryDataPoint> dataPoints;

    public HistoryResponse() {}

    // ===== Getter =====

    public Long getGreenhouseId() { return greenhouseId; }
    public Long getGroupId() { return groupId; }
    public String getSensorType() { return sensorType; }
    public String getAggregation() { return aggregation; }
    public String getUnit() { return unit; }
    public List<HistoryDataPoint> getDataPoints() { return dataPoints; }

    // ===== Setter =====

    public void setGreenhouseId(Long greenhouseId) { this.greenhouseId = greenhouseId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }
    public void setAggregation(String aggregation) { this.aggregation = aggregation; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setDataPoints(List<HistoryDataPoint> dataPoints) { this.dataPoints = dataPoints; }

    /**
     * 获取传感器类型的中文名（键与后端 SensorType 枚举一致，第 11 项修复）
     */
    public String getSensorTypeName() {
        if (sensorType == null) return "未知";
        switch (sensorType) {
            case "TEMPERATURE": return "空气温度";
            case "HUMIDITY": return "空气湿度";
            case "LIGHT": return "光照强度";
            case "CO2": return "CO₂浓度";
            case "SOIL_TEMP": return "土壤温度";
            case "SOIL_MOISTURE": return "土壤湿度";
            case "SOIL_PH": return "土壤pH";
            case "WIND_SPEED": return "风速";
            default: return sensorType;
        }
    }

    /**
     * 获取单位字符串
     */
    public String getUnitText() {
        if (unit != null && !unit.isEmpty()) return unit;
        if (sensorType == null) return "";
        switch (sensorType) {
            case "TEMPERATURE":
            case "SOIL_TEMP": return "°C";
            case "HUMIDITY":
            case "SOIL_MOISTURE": return "%";
            case "LIGHT": return "lux";
            case "CO2": return "ppm";
            case "WIND_SPEED": return "m/s";
            default: return "";
        }
    }
}
