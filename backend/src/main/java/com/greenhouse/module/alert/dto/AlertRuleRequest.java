package com.greenhouse.module.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预警规则创建/更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRuleRequest {

    @NotNull(message = "大棚ID不能为空")
    private Long greenhouseId;

    /** 传感器组ID（可空） */
    private Long groupId;

    @NotBlank(message = "传感器类型不能为空")
    private String sensorType;

    @NotBlank(message = "规则类型不能为空")
    private String ruleType;  // THRESHOLD / TREND / COMPOSITE / WEATHER

    /** 条件 JSON：{"min":15,"max":35} */
    @NotBlank(message = "规则条件不能为空")
    private String conditionJson;

    @NotBlank(message = "告警级别不能为空")
    private String alertLevel;  // INFO / WARNING / CRITICAL

    /** 关联场景ID（告警触发时自动执行） */
    private Long sceneId;

    @Builder.Default
    private Boolean enabled = true;
}
