package com.greenhouse.module.permission.dto;

import com.greenhouse.entity.EmployeePermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 员工权限详情响应 DTO
 * <p>
 * 包含员工对某个大棚的完整权限信息和该大棚的基本信息。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    private Long id;
    private Long greenhouseId;
    private String greenhouseName;

    private Boolean canViewData;
    private Boolean canControlDevice;
    private Boolean canDiagnose;
    private Boolean canAskExpert;
    private Boolean canViewAlerts;
    private Boolean canViewHistory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PermissionResponse fromEntity(EmployeePermission ep, String greenhouseName) {
        return PermissionResponse.builder()
                .id(ep.getId())
                .greenhouseId(ep.getGreenhouseId())
                .greenhouseName(greenhouseName)
                .canViewData(ep.getCanViewData())
                .canControlDevice(ep.getCanControlDevice())
                .canDiagnose(ep.getCanDiagnose())
                .canAskExpert(ep.getCanAskExpert())
                .canViewAlerts(ep.getCanViewAlerts())
                .canViewHistory(ep.getCanViewHistory())
                .createdAt(ep.getCreatedAt())
                .updatedAt(ep.getUpdatedAt())
                .build();
    }
}
