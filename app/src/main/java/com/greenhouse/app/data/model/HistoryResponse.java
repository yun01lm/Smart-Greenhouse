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

    /**
     * 获取传感器类型的中文名
     */
    public String getSensorTypeName() {
        if (sensorType == null) return "未知";
        switch (sensorType) {
            case "TEMP": return "空气温度";
            case "HUMIDITY": return "空气湿度";
            case "LIGHT": return "光照强度";
            case "CO2": return "CO₂浓度";
            case "O2": return "O₂浓度";
            case "SOIL_TEMP": return "土壤温度";
            case "SOIL_HUMIDITY": return "土壤湿度";
            case "EC": return "土壤EC值";
            case "N": return "氮(N)";
            case "P": return "磷(P)";
            case "K": return "钾(K)";
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
            case "TEMP":
            case "SOIL_TEMP": return "°C";
            case "HUMIDITY":
            case "SOIL_HUMIDITY": return "%";
            case "LIGHT": return "lux";
            case "CO2": return "ppm";
            case "O2": return "%";
            case "EC": return "mS/cm";
            case "N":
            case "P":
            case "K": return "mg/kg";
            default: return "";
        }
    }
}
