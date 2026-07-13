package com.greenhouse.module.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义阈值请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThresholdRequest {

    @NotNull(message = "大棚ID不能为空")
    private Long greenhouseId;

    /** 传感器组ID（可空） */
    private Long groupId;

    @NotBlank(message = "传感器类型不能为空")
    private String sensorType;

    private Double minThreshold;

    private Double maxThreshold;

    @Builder.Default
    private Boolean enabled = true;
}
