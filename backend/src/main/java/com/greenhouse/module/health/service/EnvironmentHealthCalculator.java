package com.greenhouse.module.health.service;

import com.greenhouse.entity.UserAlertThreshold;
import com.greenhouse.repository.UserAlertThresholdRepository;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 环境健康分计算器
 * <p>
 * 基于传感器时序数据计算大棚环境健康评分。
 * 评分维度：
 * <ul>
 *   <li><b>参数合规率 (50%)</b>：各传感器值是否在阈值范围内</li>
 *   <li><b>趋势稳定性 (30%)</b>：过去30分钟数据的波动程度</li>
 *   <li><b>组间一致性 (20%)</b>：多组传感器之间的差异程度</li>
 * </ul>
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

    /** 环境参数列表 */
    private static final List<String> SENSOR_TYPES = List.of(
            "TEMP", "HUMIDITY", "LIGHT", "CO2", "O2",
            "SOIL_TEMP", "SOIL_HUMIDITY", "EC", "N", "P", "K"
    );

    /** 系统默认阈值 (min, max)，当用户未自定义时使用 */
    private static final Map<String, double[]> DEFAULT_THRESHOLDS = Map.ofEntries(
            Map.entry("TEMP", new double[]{15.0, 35.0}),
            Map.entry("HUMIDITY", new double[]{40.0, 90.0}),
            Map.entry("LIGHT", new double[]{5000.0, 80000.0}),
            Map.entry("CO2", new double[]{300.0, 1500.0}),
            Map.entry("O2", new double[]{18.0, 22.0}),
            Map.entry("SOIL_TEMP", new double[]{15.0, 30.0}),
            Map.entry("SOIL_HUMIDITY", new double[]{30.0, 80.0}),
            Map.entry("EC", new double[]{0.5, 3.0}),
            Map.entry("N", new double[]{10.0, 200.0}),
            Map.entry("P", new double[]{10.0, 200.0}),
            Map.entry("K", new double[]{10.0, 300.0})
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

        // 1. 合规率计算 (权重50%)
        double complianceRate = calculateComplianceRate(greenhouseId, groupData);

        // 2. 趋势稳定性 (权重30%)
        double stabilityScore = calculateStability(greenhouseId);

        // 3. 组间一致性 (权重20%)
        double consistencyScore = calculateConsistency(groupData);

        double score = (complianceRate * 0.5 + stabilityScore * 0.3 + consistencyScore * 0.2) * 100;
        score = Math.max(0, Math.min(100, score));

        log.debug("大棚 {} 环境健康分: compliance={:.3f}, stability={:.3f}, consistency={:.3f}, final={:.1f}",
                greenhouseId, complianceRate, stabilityScore, consistencyScore, score);

        return score;
    }

    /**
     * 查询大棚各组传感器最新值
     * <p>
     * 返回 Map<device_id, Map<sensor_type, value>>
     * </p>
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
     * <p>
     * 对每个传感器组、每种参数：值在 [min, max] 内 → 1.0，否则按偏离度计算
     * </p>
     */
    private double calculateComplianceRate(Long greenhouseId,
                                            Map<String, Map<String, Double>> groupData) {
        // 预加载该大棚所有用户自定义阈值
        List<UserAlertThreshold> customThresholds =
                thresholdRepository.findByGreenhouseId(greenhouseId);

        double totalCompliance = 0;
        int count = 0;

        for (Map.Entry<String, Map<String, Double>> group : groupData.entrySet()) {
            String deviceId = group.getKey();
            for (Map.Entry<String, Double> sensor : group.getValue().entrySet()) {
                String sensorType = sensor.getKey();
                double value = sensor.getValue();

                // 获取有效阈值：用户自定义 > 系统默认
                double[] threshold = getEffectiveThreshold(customThresholds, deviceId, sensorType);
                if (threshold == null) continue; // 无阈值定义，跳过

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

        return count > 0 ? totalCompliance / count : 0.8; // 无数据默认0.8
    }

    /**
     * 获取传感器有效阈值
     * <p>
     * 优先级：用户自定义（按大棚+设备+传感器类型匹配）> 系统默认
     * </p>
     */
    private double[] getEffectiveThreshold(List<UserAlertThreshold> customThresholds,
                                           String deviceId, String sensorType) {
        // 查找用户自定义阈值
        for (UserAlertThreshold ct : customThresholds) {
            if (ct.getSensorType().equals(sensorType) && ct.getEnabled()) {
                // 如果自定义阈值指定了 groupId，需匹配设备ID
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

        // 回退到系统默认
        return DEFAULT_THRESHOLDS.getOrDefault(sensorType, null);
    }

    /**
     * 计算趋势稳定性
     * <p>
     * 查询过去30分钟数据，计算各参数方差。方差越小 → 稳定性越高。
     * stability_i = 1.0 / (1.0 + variance / threshold)
     * </p>
     */
    private double calculateStability(Long greenhouseId) {
        // 使用聚合窗口计算30分钟内的方差
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
            return 0.8; // 无足够数据，默认稳定
        }

        double totalStability = 0;
        int count = 0;

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String sensorType = (String) record.getValueByKey("sensor_type");
                Double stddev = (Double) record.getValue();

                if (sensorType != null && stddev != null) {
                    // 方差 = stddev²
                    double variance = stddev * stddev;
                    // 归一化：根据参数类型取参考范围
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
     * <p>
     * 各组同参数的标准差 → 标准差越小 → 一致性越高
     * consistency_i = 1.0 / (1.0 + std_dev / expected_range)
     * </p>
     */
    private double calculateConsistency(Map<String, Map<String, Double>> groupData) {
        if (groupData.size() <= 1) {
            return 1.0; // 只有一组传感器，默认完全一致
        }

        double totalConsistency = 0;
        int paramCount = 0;

        // 对每种参数，计算各组之间的标准差
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
                double consistency = 1.0 / (1.0 + stdDev / (range * 0.3)); // 允许30%范围波动
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
        double[] defaults = DEFAULT_THRESHOLDS.get(sensorType);
        if (defaults == null) return 10.0;
        return defaults[1] - defaults[0];
    }
}
