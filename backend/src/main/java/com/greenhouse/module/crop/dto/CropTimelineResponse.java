package com.greenhouse.module.crop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生长时间线响应 DTO
 * <p>
 * 按时间顺序汇总生长过程中的关键事件。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropTimelineResponse {

    /** 周期ID */
    private Long cycleId;

    /** 作物名称 */
    private String cropType;

    /** 品种 */
    private String variety;

    /** 种植日期 */
    private String plantingDate;

    /** 当前阶段 */
    private String currentStage;

    /** 种植后天数 */
    private long daysSincePlanting;

    /** 时间线事件列表 */
    private List<TimelineEvent> events;

    /**
     * 时间线事件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        /** 事件日期 */
        private String date;

        /** 事件类型：PLANTING/STAGE_CHANGE/ASSESSMENT/ALERT/HARVEST */
        private String type;

        /** 事件标题 */
        private String title;

        /** 事件描述 */
        private String description;
    }
}
