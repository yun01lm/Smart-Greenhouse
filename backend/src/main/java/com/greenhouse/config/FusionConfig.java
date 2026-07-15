package com.greenhouse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 多模态融合配置
 * <p>
 * 将环境健康、视觉健康、天气修正的所有权重、阈值和因子外部化，
 * 支持论文对比实验（修改 YAML 即可切换权重，无需重新编译）。
 * </p>
 *
 * <h3>配置前缀</h3>
 * {@code greenhouse.fusion}
 */
@Data
@Component
@ConfigurationProperties(prefix = "greenhouse.fusion")
public class FusionConfig {

    /** 环境健康评估权重 */
    private EnvWeights env = new EnvWeights();

    /** 视觉健康评估权重 */
    private VisualWeights visual = new VisualWeights();

    /** 综合融合权重 */
    private OverallWeights overall = new OverallWeights();

    /** 传感器默认阈值（min, max），当用户未自定义时使用 */
    private Map<String, double[]> defaultThresholds = Map.ofEntries(
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

    /** 病害严重性因子（0.0~1.0，越大越不严重） */
    private Map<String, Double> severityFactors = Map.of(
            "NORMAL", 1.0,
            "NUTRIENT", 0.9,
            "FUNGAL", 0.8,
            "PEST", 0.7,
            "BACTERIAL", 0.6,
            "VIRAL", 0.4
    );

    @Data
    public static class EnvWeights {
        /** 参数合规率权重（默认 0.5） */
        private double compliance = 0.5;
        /** 趋势稳定性权重（默认 0.3） */
        private double stability = 0.3;
        /** 组间一致性权重（默认 0.2） */
        private double consistency = 0.2;
    }

    @Data
    public static class VisualWeights {
        /** 病害评分权重（默认 0.6） */
        private double disease = 0.6;
        /** 长势评分权重（默认 0.4） */
        private double growth = 0.4;
    }

    @Data
    public static class OverallWeights {
        /** 环境健康权重（默认 0.6） */
        private double envWeight = 0.6;
        /** 视觉健康权重（默认 0.4） */
        private double visualWeight = 0.4;
    }
}
