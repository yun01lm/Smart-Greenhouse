package com.greenhouse.module.health.service;

import com.greenhouse.entity.DiagnosticRecord;
import com.greenhouse.repository.DiagnosticRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 视觉健康分计算器
 * <p>
 * 基于病虫害诊断结果和作物长势评估计算视觉维度的健康评分。
 * 评分维度：
 * <ul>
 *   <li><b>病害评分 (60%)</b>：基于最近24小时诊断记录</li>
 *   <li><b>长势评分 (40%)</b>：基于最近7天长势评估（预留接口）</li>
 * </ul>
 * </p>
 *
 * <h3>病害严重性因子</h3>
 * <table>
 *   <tr><td>FUNGAL (真菌)</td><td>0.8</td><td>可控制</td></tr>
 *   <tr><td>BACTERIAL (细菌)</td><td>0.6</td><td>较严重</td></tr>
 *   <tr><td>VIRAL (病毒)</td><td>0.4</td><td>很严重</td></tr>
 *   <tr><td>PEST (虫害)</td><td>0.7</td><td>中等</td></tr>
 *   <tr><td>NUTRIENT (营养)</td><td>0.9</td><td>较易纠正</td></tr>
 *   <tr><td>NORMAL (正常)</td><td>1.0</td><td>健康</td></tr>
 * </table>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisualHealthCalculator {

    private final DiagnosticRecordRepository diagnosticRecordRepository;

    /**
     * 计算视觉健康分
     *
     * @param greenhouseId 大棚ID
     * @return 视觉健康分 (0-100)，数据缺失时返回默认值 80.0
     */
    public double calculate(Long greenhouseId) {
        // 1. 病害评分 (权重60%)
        double diseaseScore = calculateDiseaseScore(greenhouseId);

        // 2. 长势评分 (权重40%)
        // 注：长势评估模块 (F7) 在 Phase 4 开发，当前使用默认值
        double growthScore = calculateGrowthScore(greenhouseId);

        double score = (diseaseScore * 0.6 + growthScore * 0.4) * 100;
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
            return 0.8; // 无诊断记录，默认基本健康
        }

        // 取最新诊断结果
        DiagnosticRecord latest = records.get(0);

        // 如果识别引擎为空或没有病害名称，视为正常
        if (latest.getDiseaseName() == null || latest.getDiseaseName().isBlank()) {
            return 1.0;
        }

        // 根据病害名称推断类别
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
        // GrowthAssessment latest = growthAssessmentRepository
        //         .findTopByGreenhouseIdOrderByCreatedAtDesc(greenhouseId);
        // if (latest != null) {
        //     return latest.getHealthScore() / 100.0;
        // }
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

        // 无法推断，默认为真菌类（最常见的病害类型）
        return "FUNGAL";
    }

    /**
     * 获取病害严重性因子
     */
    private double getSeverityFactor(String category) {
        return switch (category) {
            case "NORMAL" -> 1.0;
            case "NUTRIENT" -> 0.9;
            case "FUNGAL" -> 0.8;
            case "PEST" -> 0.7;
            case "BACTERIAL" -> 0.6;
            case "VIRAL" -> 0.4;
            default -> 0.5;
        };
    }
}
