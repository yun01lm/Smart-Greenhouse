package com.greenhouse.module.crop.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.CropCycle;
import com.greenhouse.module.crop.dto.CropCycleRequest;
import com.greenhouse.module.crop.dto.CropCycleResponse;
import com.greenhouse.module.crop.dto.CropTimelineResponse;
import com.greenhouse.repository.CropCycleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 作物生长周期服务
 * <p>
 * 管理种植记录的增删改查、阶段自动估算和生长时间线。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CropCycleService {

    private final CropCycleRepository repository;

    /**
     * 创建种植记录
     *
     * @param request 种植信息
     * @return 创建后的周期
     */
    @Transactional
    public CropCycleResponse create(CropCycleRequest request) {
        // 检查该大棚是否已有进行中的周期
        repository.findTopByGreenhouseIdAndStatus(request.getGreenhouseId(), CropCycle.CycleStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.CROP_CYCLE_DUPLICATE);
                });

        // 创建周期
        CropCycle cycle = CropCycle.builder()
                .greenhouseId(request.getGreenhouseId())
                .cropType(request.getCropType())
                .variety(request.getVariety())
                .plantingDate(request.getPlantingDate())
                .expectedHarvestDate(request.getExpectedHarvestDate())
                .currentStage("育苗期")
                .stageSource(CropCycle.StageSource.AUTO)
                .status(CropCycle.CycleStatus.ACTIVE)
                .notes(request.getNotes())
                .build();

        // 自动估算当前阶段
        cycle.autoUpdateStage();

        cycle = repository.save(cycle);
        log.info("种植记录已创建: id={}, greenhouseId={}, crop={}, stage={}",
                cycle.getId(), cycle.getGreenhouseId(), cycle.getCropType(), cycle.getCurrentStage());

        return CropCycleResponse.fromEntity(cycle);
    }

    /**
     * 查询生长周期列表
     */
    public List<CropCycleResponse> list(Long greenhouseId, CropCycle.CycleStatus status, int page, int size) {
        Page<CropCycle> cycles;
        if (status != null) {
            cycles = repository.findByGreenhouseIdAndStatus(greenhouseId, status, PageRequest.of(page, size));
        } else {
            cycles = repository.findByGreenhouseId(greenhouseId, PageRequest.of(page, size));
        }

        // 自动更新每个周期的阶段
        List<CropCycle> updatedList = new ArrayList<>();
        for (CropCycle cycle : cycles.getContent()) {
            cycle.autoUpdateStage();
            updatedList.add(cycle);
        }
        repository.saveAll(updatedList);

        return updatedList.stream()
                .map(CropCycleResponse::fromEntity)
                .toList();
    }

    /**
     * 查询周期详情
     */
    public CropCycleResponse getById(Long id) {
        CropCycle cycle = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CROP_CYCLE_NOT_FOUND));

        // 自动更新阶段
        cycle.autoUpdateStage();
        cycle = repository.save(cycle);

        return CropCycleResponse.fromEntity(cycle);
    }

    /**
     * 更新周期（推进阶段等）
     */
    @Transactional
    public CropCycleResponse update(Long id, CropCycleRequest request) {
        CropCycle cycle = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CROP_CYCLE_NOT_FOUND));

        if (cycle.getStatus() != CropCycle.CycleStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CROP_CYCLE_ALREADY_COMPLETED);
        }

        // 更新基本信息
        if (request.getCropType() != null) cycle.setCropType(request.getCropType());
        if (request.getVariety() != null) cycle.setVariety(request.getVariety());
        if (request.getPlantingDate() != null) cycle.setPlantingDate(request.getPlantingDate());
        if (request.getExpectedHarvestDate() != null) cycle.setExpectedHarvestDate(request.getExpectedHarvestDate());
        if (request.getNotes() != null) cycle.setNotes(request.getNotes());

        cycle.autoUpdateStage();
        cycle = repository.save(cycle);

        return CropCycleResponse.fromEntity(cycle);
    }

    /**
     * 标记完成（收获）
     */
    @Transactional
    public CropCycleResponse complete(Long id) {
        CropCycle cycle = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CROP_CYCLE_NOT_FOUND));

        if (cycle.getStatus() != CropCycle.CycleStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CROP_CYCLE_ALREADY_COMPLETED);
        }

        cycle.setStatus(CropCycle.CycleStatus.COMPLETED);
        cycle.setCurrentStage("收获期");
        cycle.setStageSource(CropCycle.StageSource.MANUAL);
        cycle.setActualHarvestDate(LocalDate.now());

        cycle = repository.save(cycle);
        log.info("生长周期已标记完成: id={}, greenhouseId={}, crop={}",
                cycle.getId(), cycle.getGreenhouseId(), cycle.getCropType());

        return CropCycleResponse.fromEntity(cycle);
    }

    /**
     * 手动设置生长阶段
     */
    @Transactional
    public CropCycleResponse setStage(Long id, String stage) {
        CropCycle cycle = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CROP_CYCLE_NOT_FOUND));

        if (cycle.getStatus() != CropCycle.CycleStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CROP_CYCLE_ALREADY_COMPLETED);
        }

        // 校验阶段名称
        boolean valid = false;
        for (String s : CropCycle.STANDARD_STAGES) {
            if (s.equals(stage)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        cycle.setCurrentStage(stage);
        cycle.setStageSource(CropCycle.StageSource.MANUAL);
        cycle = repository.save(cycle);

        return CropCycleResponse.fromEntity(cycle);
    }

    /**
     * 获取生长时间线
     * <p>
     * 汇总种植、阶段变更等关键事件。Phase 4 将关联长势评估和预警记录。
     * </p>
     */
    public CropTimelineResponse getTimeline(Long id) {
        CropCycle cycle = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CROP_CYCLE_NOT_FOUND));

        cycle.autoUpdateStage();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<CropTimelineResponse.TimelineEvent> events = new ArrayList<>();

        // 种植事件
        events.add(CropTimelineResponse.TimelineEvent.builder()
                .date(cycle.getPlantingDate().format(fmt))
                .type("PLANTING")
                .title("种植 " + cycle.getCropType())
                .description(cycle.getVariety() != null ? "品种：" + cycle.getVariety() : "")
                .build());

        // 阶段变更事件（基于自动估算标记）
        long totalDays = cycle.getDaysSincePlanting();
        String[] stageNames = {"育苗期", "生长期", "开花期", "结果期"};
        int[] stageDays = {20, 40, 55, 80};

        for (int i = 0; i < stageNames.length; i++) {
            if (totalDays >= stageDays[i]) {
                LocalDate stageDate = cycle.getPlantingDate().plusDays(stageDays[i]);
                events.add(CropTimelineResponse.TimelineEvent.builder()
                        .date(stageDate.format(fmt))
                        .type("STAGE_CHANGE")
                        .title("进入" + stageNames[i])
                        .description("种植后第" + stageDays[i] + "天，自动估算")
                        .build());
            }
        }

        // 如果已完成，添加收获事件
        if (cycle.getStatus() == CropCycle.CycleStatus.COMPLETED && cycle.getActualHarvestDate() != null) {
            events.add(CropTimelineResponse.TimelineEvent.builder()
                    .date(cycle.getActualHarvestDate().format(fmt))
                    .type("HARVEST")
                    .title("收获 " + cycle.getCropType())
                    .description("实际收获日期")
                    .build());
        }

        return CropTimelineResponse.builder()
                .cycleId(cycle.getId())
                .cropType(cycle.getCropType())
                .variety(cycle.getVariety())
                .plantingDate(cycle.getPlantingDate().format(fmt))
                .currentStage(cycle.getCurrentStage())
                .daysSincePlanting(totalDays)
                .events(events)
                .build();
    }
}
