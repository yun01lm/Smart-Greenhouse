package com.greenhouse.module.device.dto;

import com.greenhouse.entity.DeviceGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备分组响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceGroupResponse {

    private Long id;
    private String name;
    private Long greenhouseId;
    private String description;
    private List<Long> deviceIds;
    private int deviceCount;
    private LocalDateTime createdAt;

    public static DeviceGroupResponse fromEntity(DeviceGroup g) {
        return DeviceGroupResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .greenhouseId(g.getGreenhouseId())
                .description(g.getDescription())
                .deviceIds(g.getDeviceIds())
                .deviceCount(g.getDeviceIds() != null ? g.getDeviceIds().size() : 0)
                .createdAt(g.getCreatedAt())
                .build();
    }
}
