package com.greenhouse.module.alert.dto;

import com.greenhouse.entity.AlertRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 预警规则响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRuleResponse {

    private Long id;
    private Long greenhouseId;
    private Long groupId;
    private String sensorType;
    private String ruleType;
    private String conditionJson;
    private String alertLevel;
    private Long sceneId;
    private Boolean enabled;
    private LocalDateTime createdAt;

    public static AlertRuleResponse fromEntity(AlertRule rule) {
        return AlertRuleResponse.builder()
                .id(rule.getId())
                .greenhouseId(rule.getGreenhouseId())
                .groupId(rule.getGroupId())
                .sensorType(rule.getSensorType())
                .ruleType(rule.getRuleType().name())
                .conditionJson(rule.getConditionJson())
                .alertLevel(rule.getAlertLevel().name())
                .sceneId(rule.getSceneId())
                .enabled(rule.getEnabled())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
