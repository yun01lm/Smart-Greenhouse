package com.greenhouse.module.sensor.service;

import com.greenhouse.module.sensor.dto.ForecastPoint;
import com.greenhouse.module.sensor.dto.SensorDataPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计趋势预测实现（第一阶段）
 * <p>
 * 对最近 WINDOW 个历史点做最小二乘线性回归求每小时趋势斜率，
 * 以最后一个观测值为基准做带阻尼的外推（越远可信度越低），
 * 并将预测值钳制在传感器合理范围内，避免异常漂移。
 * 后续 LSTM 模型训练完成后替换 {@link TrendPredictor} 实现即可。
 * </p>
 */
@Slf4j
@Component
public class StatisticalTrendPredictor implements TrendPredictor {

    /** 参与回归的历史窗口大小 */
    private static final int WINDOW = 12;

    /** 阻尼系数：每往后一步，趋势影响衰减为前一步的 90% */
    private static final double DECAY = 0.9;

    /** 各传感器类型的合理取值范围（预测钳制） */
    private static final Map<String, double[]> BOUNDS = new HashMap<>();
    static {
        BOUNDS.put("TEMPERATURE", new double[]{-20, 60});
        BOUNDS.put("SOIL_TEMP", new double[]{-10, 45});
        BOUNDS.put("HUMIDITY", new double[]{0, 100});
        BOUNDS.put("SOIL_MOISTURE", new double[]{0, 100});
        BOUNDS.put("CO2", new double[]{0, 5000});
        BOUNDS.put("LIGHT", new double[]{0, 200000});
        BOUNDS.put("SOIL_PH", new double[]{0, 14});
        BOUNDS.put("WIND_SPEED", new double[]{0, 50});
    }

    @Override
    public List<ForecastPoint> predict(String sensorType, List<SensorDataPoint> history,
                                       int steps, long intervalMs) {
        List<ForecastPoint> result = new ArrayList<>();
        if (history == null || steps <= 0 || intervalMs <= 0) {
            return result;
        }

        List<SensorDataPoint> points = history.stream()
                .filter(p -> p != null && p.getValue() != null && p.getTimestamp() != null)
                .sorted(Comparator.comparing(SensorDataPoint::getTimestamp))
                .toList();
        if (points.size() < 2) {
            return result;
        }

        List<SensorDataPoint> window = points.subList(Math.max(0, points.size() - WINDOW), points.size());
        SensorDataPoint last = window.get(window.size() - 1);
        double slope = linearRegressionSlopePerHour(window);
        double baseValue = last.getValue();
        long lastTs = last.getTimestamp().toEpochMilli();
        double[] bounds = BOUNDS.getOrDefault(sensorType, new double[]{Double.MIN_VALUE, Double.MAX_VALUE});

        for (int i = 1; i <= steps; i++) {
            double hours = i * intervalMs / 3600000.0;
            double decay = Math.pow(DECAY, i);
            double value = baseValue + slope * hours * decay;
            value = Math.max(bounds[0], Math.min(bounds[1], value));
            result.add(ForecastPoint.builder()
                    .timestamp(Instant.ofEpochMilli(lastTs + i * intervalMs))
                    .value(Math.round(value * 100.0) / 100.0)
                    .build());
        }
        return result;
    }

    /** 最小二乘线性回归，返回每小时趋势斜率（单位：值/小时） */
    private double linearRegressionSlopePerHour(List<SensorDataPoint> pts) {
        int n = pts.size();
        double t0 = pts.get(0).getTimestamp().toEpochMilli();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (SensorDataPoint p : pts) {
            double x = (p.getTimestamp().toEpochMilli() - t0) / 3600000.0;
            double y = p.getValue();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        double denom = n * sumXX - sumX * sumX;
        if (Math.abs(denom) < 1e-9) {
            return 0.0;
        }
        return (n * sumXY - sumX * sumY) / denom;
    }
}