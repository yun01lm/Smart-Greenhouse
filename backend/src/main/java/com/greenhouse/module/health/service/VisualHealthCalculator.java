package com.greenhouse.module.health.service;

import com.greenhouse.config.FusionConfig;
import com.greenhouse.entity.DiagnosticRecord;
import com.greenhouse.repository.DiagnosticRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 视觉健康分计算器
 * <p>
 * 基于病虫害诊断结果和作物长势评估计算视觉维度的健康评分。
 * 评分维度和病害严重性因子由 {@link FusionConfig} 统一管理。
 * </p>
 *
 * <h3>评分维度（权重可通过 application.yml 配置）</h3>
 * <ul>
 *   <li><b>病害评分</b>：基于最近24小时诊断记录</li>
 *   <li><b>长势评分</b>：基于最近7天长势评估（预留接口）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisualHealthCalculator {

    private final DiagnosticRecordRepository diagnosticRecordRepository;
    private final FusionConfig fusionConfig;

    /**
     * 计算视觉健康分
     *
     * @param greenhouseId 大棚ID
     * @return 视觉健康分 (0-100)，数据缺失时返回默认值 80.0
     */
    public double calculate(Long greenhouseId) {
        FusionConfig.VisualWeights weights = fusionConfig.getVisual();

        // 1. 病害评分
        double diseaseScore = calculateDiseaseScore(greenhouseId);

        // 2. 长势评分
        // 注：长势评估模块 (F7) 在 Phase 4 开发，当前使用默认值
        double growthScore = calculateGrowthScore(greenhouseId);

        double score = (diseaseScore * weights.getDisease()
                + growthScore * weights.getGrowth()) * 100;
        score = Math.max(0, Math.min(100, score));

        log.debug("大棚 {} 视觉健康分: disease={:.3f}, growth={:.3f}, final={:.1f}",
                greenhouseId, diseaseScore, growthScore, score);

        return score;
    }

    /**
     * 病害评分
     * <p>
     * 查询最近24小时诊断记录：
     * - 无记录 → 默认 0.8
     * - NORMAL → 1.0
     * - 有病害 → 1.0 - (1.0 - confidence) * (1.0 - severity_factor)
     * </p>
     */
    private double calculateDiseaseScore(Long greenhouseId) {
        List<DiagnosticRecord> records =
                diagnosticRecordRepository.findByGreenhouseIdOrderByCreatedAtDesc(greenhouseId);

        if (records.isEmpty()) {
            return 0.8;
        }

        DiagnosticRecord latest = records.get(0);

        if (latest.getDiseaseName() == null || latest.getDiseaseName().isBlank()) {
            return 1.0;
        }

        String diseaseCategory = inferDiseaseCategory(latest.getDiseaseName());
        double severityFactor = getSeverityFactor(diseaseCategory);

        double confidence = latest.getConfidence() != null ? latest.getConfidence() : 0.5;
        double score = 1.0 - (1.0 - confidence) * (1.0 - severityFactor);

        return Math.max(0, score);
    }

    /**
     * 长势评分
     * <p>
     * Phase 4 将接入 growth_assessments 表。
     * 当前返回默认值 0.8。
     * </p>
     */
    private double calculateGrowthScore(Long greenhouseId) {
        // TODO Phase 4: 接入 GrowthAssessmentService
        return 0.8;
    }

    /**
     * 根据病害名称推断病害类别
     */
    private String inferDiseaseCategory(String diseaseName) {
        String lower = diseaseName.toLowerCase();

        if (lower.contains("霉") || lower.contains("真菌") || lower.contains("fungus") ||
                lower.contains("白粉") || lower.contains("锈") || lower.contains("灰霉")) {
            return "FUNGAL";
        }
        if (lower.contains("细菌") || lower.contains("bacterial") || lower.contains("斑") ||
                lower.contains("软腐") || lower.contains("溃疡")) {
            return "BACTERIAL";
        }
        if (lower.contains("病毒") || lower.contains("viral") || lower.contains("花叶") ||
                lower.contains("黄化")) {
            return "VIRAL";
        }
        if (lower.contains("虫") || lower.contains("pest") || lower.contains("蚜") ||
                lower.contains("螨") || lower.contains("蛾") || lower.contains("虱")) {
            return "PEST";
        }
        if (lower.contains("缺") || lower.contains("营养") || lower.contains("nutrient") ||
                lower.contains("氮") || lower.contains("磷") || lower.contains("钾")) {
            return "NUTRIENT";
        }

        return "FUNGAL";
    }

    /**
     * 获取病害严重性因子（从 FusionConfig 读取）
     */
    private double getSeverityFactor(String category) {
        Map<String, Double> factors = fusionConfig.getSeverityFactors();
        return factors.getOrDefault(category, 0.5);
    }
}
