package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 历史数据点模型
 * <p>
 * 对应后端: GET /api/v1/sensors/history → dataPoints[]
 * </p>
 */
public class HistoryDataPoint {

    private String time;
    private Double avg;
    private Double min;
    private Double max;

    public HistoryDataPoint() {}

    // ===== Getter =====

    public String getTime() { return time; }
    public Double getAvg() { return avg; }
    public Double getMin() { return min; }
    public Double getMax() { return max; }

    /**
     * 获取主要展示值（默认平均值）
     */
    public Double getValue() {
        return avg != null ? avg : 0.0;
    }
}
