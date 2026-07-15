package com.greenhouse.module.health.service;

import com.greenhouse.config.FusionConfig;
import com.greenhouse.entity.UserAlertThreshold;
import com.greenhouse.repository.UserAlertThresholdRepository;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 环境健康分计算器
 * <p>
 * 基于传感器时序数据计算大棚环境健康评分。
 * 评分维度（权重可通过 application.yml 配置）：
 * <ul>
 *   <li><b>参数合规率</b>：各传感器值是否在阈值范围内</li>
 *   <li><b>趋势稳定性</b>：过去30分钟数据的波动程度</li>
 *   <li><b>组间一致性</b>：多组传感器之间的差异程度</li>
 * </ul>
 * 权重和默认阈值由 {@link FusionConfig} 统一管理。
 * </p>
 *
 * <h3>11种传感器参数</h3>
 * TEMP, HUMIDITY, LIGHT, CO2, O2, SOIL_TEMP, SOIL_HUMIDITY, EC, N, P, K
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentHealthCalculator {

    private final QueryApi queryApi;
    private final UserAlertThresholdRepository thresholdRepository;
    private final FusionConfig fusionConfig;

    /** 环境参数列表 */
    private static final List<String> SENSOR_TYPES = List.of(
            "TEMP", "HUMIDITY", "LIGHT", "CO2", "O2",
            "SOIL_TEMP", "SOIL_HUMIDITY", "EC", "N", "P", "K"
    );

    /**
     * 计算环境健康分
     *
     * @param greenhouseId 大棚ID
     * @return 环境健康分 (0-100)，数据缺失时返回默认值 80.0
     */
    public double calculate(Long greenhouseId) {
        // 获取各组最新传感器数据
        Map<String, Map<String, Double>> groupData = queryLatestByGroup(greenhouseId);

        if (groupData.isEmpty()) {
            log.warn("大棚 {} 无传感器数据，环境健康分使用默认值 80.0", greenhouseId);
            return 80.0;
        }

        FusionConfig.EnvWeights weights = fusionConfig.getEnv();

        // 1. 合规率计算
        double complianceRate = calculateComplianceRate(greenhouseId, groupData);

        // 2. 趋势稳定性
        double stabilityScore = calculateStability(greenhouseId);

        // 3. 组间一致性
        double consistencyScore = calculateConsistency(groupData);

        double score = (complianceRate * weights.getCompliance()
                + stabilityScore * weights.getStability()
                + consistencyScore * weights.getConsistency()) * 100;
        score = Math.max(0, Math.min(100, score));

        log.debug("大棚 {} 环境健康分: compliance={:.3f}, stability={:.3f}, consistency={:.3f}, final={:.1f}",
                greenhouseId, complianceRate, stabilityScore, consistencyScore, score);

        return score;
    }

    /**
     * 查询大棚各组传感器最新值
     */
    private Map<String, Map<String, Double>> queryLatestByGroup(Long greenhouseId) {
        String flux = String.format(
                "from(bucket: \"sensor_data\") " +
                        "|> range(start: -5m) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                        "|> filter(fn: (r) => r[\"greenhouse_id\"] == \"%d\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"value\") " +
                        "|> last()",
                greenhouseId
        );

        List<FluxTable> tables = queryApi.query(flux);
        Map<String, Map<String, Double>> result = new LinkedHashMap<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String deviceId = (String) record.getValueByKey("device_id");
                String sensorType = (String) record.getValueByKey("sensor_type");
                Double value = (Double) record.getValue();

                if (deviceId != null && sensorType != null && value != null) {
                    result.computeIfAbsent(deviceId, k -> new HashMap<>())
                            .put(sensorType, value);
                }
            }
        }

        return result;
    }

    /**
     * 计算参数合规率
     */
    private double calculateComplianceRate(Long greenhouseId,
                                            Map<String, Map<String, Double>> groupData) {
        List<UserAlertThreshold> customThresholds =
                thresholdRepository.findByGreenhouseId(greenhouseId);

        double totalCompliance = 0;
        int count = 0;

        for (Map.Entry<String, Map<String, Double>> group : groupData.entrySet()) {
            String deviceId = group.getKey();
            for (Map.Entry<String, Double> sensor : group.getValue().entrySet()) {
                String sensorType = sensor.getKey();
                double value = sensor.getValue();

                double[] threshold = getEffectiveThreshold(customThresholds, deviceId, sensorType);
                if (threshold == null) continue;

                double min = threshold[0];
                double max = threshold[1];
                double compliance;

                if (value < min) {
                    compliance = Math.max(0, value / min);
                } else if (value > max) {
                    compliance = Math.max(0, max / value);
                } else {
                    compliance = 1.0;
                }

                totalCompliance += compliance;
                count++;
            }
        }

        return count > 0 ? totalCompliance / count : 0.8;
    }

    /**
     * 获取传感器有效阈值
     * 优先级：用户自定义 > 系统默认（从 FusionConfig 读取）
     */
    private double[] getEffectiveThreshold(List<UserAlertThreshold> customThresholds,
                                           String deviceId, String sensorType) {
        for (UserAlertThreshold ct : customThresholds) {
            if (ct.getSensorType().equals(sensorType) && ct.getEnabled()) {
                if (ct.getGroupId() != null && !String.valueOf(ct.getGroupId()).equals(deviceId)) {
                    continue;
                }
                Double min = ct.getMinThreshold();
                Double max = ct.getMaxThreshold();
                if (min != null && max != null) {
                    return new double[]{min, max};
                }
            }
        }

        return fusionConfig.getDefaultThresholds().getOrDefault(sensorType, null);
    }

    /**
     * 计算趋势稳定性
     */
    private double calculateStability(Long greenhouseId) {
        String flux = String.format(
                "from(bucket: \"sensor_data\") " +
                        "|> range(start: -30m) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"sensor_data\") " +
                        "|> filter(fn: (r) => r[\"greenhouse_id\"] == \"%d\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"value\") " +
                        "|> group(columns: [\"sensor_type\", \"device_id\"]) " +
                        "|> aggregateWindow(every: 5m, fn: mean, createEmpty: false) " +
                        "|> stddev()",
                greenhouseId
        );

        List<FluxTable> tables = queryApi.query(flux);

        if (tables.isEmpty()) {
            return 0.8;
        }

        double totalStability = 0;
        int count = 0;

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String sensorType = (String) record.getValueByKey("sensor_type");
                Double stddev = (Double) record.getValue();

                if (sensorType != null && stddev != null) {
                    double variance = stddev * stddev;
                    double range = getParameterRange(sensorType);
                    double stability = 1.0 / (1.0 + variance / (range * range));
                    totalStability += stability;
                    count++;
                }
            }
        }

        return count > 0 ? totalStability / count : 0.8;
    }

    /**
     * 计算组间一致性
     */
    private double calculateConsistency(Map<String, Map<String, Double>> groupData) {
        if (groupData.size() <= 1) {
            return 1.0;
        }

        double totalConsistency = 0;
        int paramCount = 0;

        for (String sensorType : SENSOR_TYPES) {
            List<Double> values = new ArrayList<>();
            for (Map<String, Double> sensors : groupData.values()) {
                Double val = sensors.get(sensorType);
                if (val != null) {
                    values.add(val);
                }
            }

            if (values.size() >= 2) {
                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double variance = values.stream()
                        .mapToDouble(v -> (v - mean) * (v - mean))
                        .average().orElse(0);
                double stdDev = Math.sqrt(variance);
                double range = getParameterRange(sensorType);
                double consistency = 1.0 / (1.0 + stdDev / (range * 0.3));
                totalConsistency += consistency;
                paramCount++;
            }
        }

        return paramCount > 0 ? totalConsistency / paramCount : 1.0;
    }

    /**
     * 获取参数参考范围（用于方差归一化）
     */
    private double getParameterRange(String sensorType) {
        double[] defaults = fusionConfig.getDefaultThresholds().get(sensorType);
        if (defaults == null) return 10.0;
        return defaults[1] - defaults[0];
    }
}
