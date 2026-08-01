package com.greenhouse.module.growth.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.common.PageResult;
import com.greenhouse.entity.GrowthAssessment;
import com.greenhouse.module.growth.dto.GrowthImageResponse;
import com.greenhouse.module.growth.dto.GrowthResponse;
import com.greenhouse.repository.GrowthAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 长势评估服务
 * <p>
 * 提供长势评估记录查询、截帧图片列表。
 * 当前数据来源为数据库已有记录；后续对接 AI 视觉分析后可自动生成评估数据。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthService {

    private final GrowthAssessmentRepository growthAssessmentRepository;

    /**
     * 获取大棚最新长势评估
     */
    public GrowthResponse getLatest(Long greenhouseId) {
        GrowthAssessment latest = growthAssessmentRepository
                .findTopByGreenhouseIdOrderByCreatedAtDesc(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "暂无长势评估数据"));
        return GrowthResponse.fromEntity(latest);
    }

    /**
     * 获取长势评估历史（分页）
     */
    public PageResult<GrowthResponse> getHistory(Long greenhouseId, int page, int size) {
        Page<GrowthAssessment> assessments = growthAssessmentRepository
                .findByGreenhouseIdOrderByCreatedAtDesc(
                        greenhouseId,
                        PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                );

        List<GrowthResponse> list = assessments.getContent().stream()
                .map(GrowthResponse::fromEntity)
                .toList();

        return PageResult.<GrowthResponse>builder()
                .list(list)
                .total(assessments.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 获取截帧图片列表（分页，按日期筛选）
     * <p>
     * 当前从 GrowthAssessment 中提取 imagePath 非空的记录。
     * 后续接 FFmpeg 后可改为从独立图片表查询。
     * </p>
     */
    public PageResult<GrowthImageResponse> getImages(Long greenhouseId, int page, int size) {
        Page<GrowthAssessment> assessments = growthAssessmentRepository
                .findByGreenhouseIdAndImagePathIsNotNullOrderByCreatedAtDesc(
                        greenhouseId,
                        PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                );

        List<GrowthImageResponse> list = assessments.getContent().stream()
                .map(GrowthImageResponse::fromEntity)
                .toList();

        return PageResult.<GrowthImageResponse>builder()
                .list(list)
                .total(assessments.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }
}
