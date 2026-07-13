package com.greenhouse.module.health.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.PageResult;
import com.greenhouse.entity.HealthAssessment;
import com.greenhouse.module.health.dto.HealthHistoryResponse;
import com.greenhouse.module.health.dto.HealthScoreResponse;
import com.greenhouse.module.health.service.HealthAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康综合评估控制器
 * <p>
 * 提供多模态融合健康评分的查询接口。
 * 权限要求：OWNER / WORKER（通过 Spring Security 控制）
 * </p>
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthAssessmentService healthAssessmentService;

    /**
     * 获取当前综合健康评分
     * <p>
     * 手动触发：实时计算并返回最新评估结果。
     * 如果最近30分钟内有缓存，优先返回缓存。
     * </p>
     *
     * @param greenhouseId 大棚ID（必填）
     */
    @GetMapping("/score")
    public ApiResponse<HealthScoreResponse> getCurrentScore(
            @RequestParam Long greenhouseId) {
        // 先检查缓存（30分钟内）
        HealthAssessment cached = healthAssessmentService.getCurrentScore(greenhouseId);
        if (cached != null && cached.getCreatedAt()
                .isAfter(LocalDateTime.now().minusMinutes(30))) {
            return ApiResponse.success(HealthScoreResponse.fromEntity(cached));
        }

        // 无缓存或已过期，重新计算
        HealthAssessment assessment = healthAssessmentService.calculateAndSave(greenhouseId);
        return ApiResponse.success(HealthScoreResponse.fromEntity(assessment));
    }

    /**
     * 查询健康评分历史（分页）
     *
     * @param greenhouseId 大棚ID（必填）
     * @param startDate    开始日期（可选，默认7天前）
     * @param endDate      结束日期（可选，默认当前时间）
     * @param page         页码（默认1）
     * @param size         每页大小（默认10）
     */
    @GetMapping("/history")
    public ApiResponse<PageResult<HealthHistoryResponse>> getHistory(
            @RequestParam Long greenhouseId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 默认时间范围：最近7天
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        Page<HealthAssessment> resultPage = healthAssessmentService.getHistory(
                greenhouseId, startDate, endDate, page, size);

        List<HealthHistoryResponse> list = resultPage.getContent().stream()
                .map(HealthHistoryResponse::fromEntity)
                .toList();

        PageResult<HealthHistoryResponse> pageResult = new PageResult<>(
                list, resultPage.getTotalElements(), page, size);

        return ApiResponse.success(pageResult);
    }

    /**
     * 获取详细评估报告
     *
     * @param id 评估记录ID
     */
    @GetMapping("/detail/{id}")
    public ApiResponse<HealthScoreResponse> getDetail(@PathVariable Long id) {
        HealthAssessment assessment = healthAssessmentService.getDetail(id);
        return ApiResponse.success(HealthScoreResponse.fromEntity(assessment));
    }
}
