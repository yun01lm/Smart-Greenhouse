package com.greenhouse.module.alert.dto;

import com.greenhouse.entity.UserAlertThreshold;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 自定义阈值响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThresholdResponse {

    private Long id;
    private Long userId;
    private Long greenhouseId;
    private Long groupId;
    private String sensorType;
    private Double minThreshold;
    private Double maxThreshold;
    private Boolean enabled;
    private LocalDateTime createdAt;

    public static ThresholdResponse fromEntity(UserAlertThreshold t) {
        return ThresholdResponse.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .greenhouseId(t.getGreenhouseId())
                .groupId(t.getGroupId())
                .sensorType(t.getSensorType())
                .minThreshold(t.getMinThreshold())
                .maxThreshold(t.getMaxThreshold())
                .enabled(t.getEnabled())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
