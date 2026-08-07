package com.greenhouse.module.sensor.service;

import com.greenhouse.module.sensor.dto.ForecastPoint;
import com.greenhouse.module.sensor.dto.SensorDataPoint;

import java.util.List;

/**
 * 环境参数短期预测 Provider
 * <p>
 * 规划分阶段实施：第一阶段用统计方法（移动平均 + 趋势外推），
 * 第二阶段在 Colab 训练 LSTM 模型后替换实现，因此统一通过本接口接入。
 * </p>
 */
public interface TrendPredictor {

    /**
     * 基于历史数据预测未来 N 个时间点
     *
     * @param sensorType 传感器类型（用于合理范围钳制）
     * @param history    历史数据（自动按时间升序过滤）
     * @param steps      预测步数
     * @param intervalMs 每步间隔（毫秒）
     * @return 预测数据点列表（可能为空）
     */
    List<ForecastPoint> predict(String sensorType, List<SensorDataPoint> history,
                                int steps, long intervalMs);
}