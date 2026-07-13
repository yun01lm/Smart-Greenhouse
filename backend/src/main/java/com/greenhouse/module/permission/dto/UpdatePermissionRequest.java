package com.greenhouse.module.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新员工权限请求 DTO
 * <p>
 * 棚主修改员工对指定大棚的功能权限。
 * 所有字段均为可选，传入的字段才会更新。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePermissionRequest {

    private Long greenhouseId;

    private Boolean canViewData;
    private Boolean canControlDevice;
    private Boolean canDiagnose;
    private Boolean canAskExpert;
    private Boolean canViewAlerts;
    private Boolean canViewHistory;
}
