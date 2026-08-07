package com.greenhouse.module.alert.dto;

import com.greenhouse.entity.Alert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 预警记录响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse {

    private Long id;
    private Long greenhouseId;
    private String greenhouseName;
    private Long groupId;
    private Long alertRuleId;
    private String level;
    private String title;
    private String content;
    private Double sensorValue;
    private String sensorType;
    private Boolean readStatus;
    private Boolean handled;
    private LocalDateTime createdAt;

    public static AlertResponse fromEntity(Alert alert, String greenhouseName) {
        return AlertResponse.builder()
                .id(alert.getId())
                .greenhouseId(alert.getGreenhouseId())
                .greenhouseName(greenhouseName)
                .groupId(alert.getGroupId())
                .alertRuleId(alert.getAlertRuleId())
                .level(alert.getLevel().name())
                .title(alert.getTitle())
                .content(alert.getContent())
                .sensorValue(alert.getSensorValue())
                .sensorType(alert.getSensorType())
                .readStatus(alert.getReadStatus())
                .handled(alert.getHandled())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
