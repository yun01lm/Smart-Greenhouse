package com.greenhouse.module.permission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加员工请求 DTO
 * <p>
 * 棚主通过用户名或手机号邀请员工，同时分配初始权限。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddEmployeeRequest {

    /** 员工的用户名或手机号 */
    @NotBlank(message = "用户名或手机号不能为空")
    private String identifier;

    /** 授权的大棚ID */
    private Long greenhouseId;

    /** 查看数据权限 */
    @Builder.Default
    private Boolean canViewData = true;

    /** 控制设备权限 */
    @Builder.Default
    private Boolean canControlDevice = false;

    /** 病虫害诊断权限 */
    @Builder.Default
    private Boolean canDiagnose = false;

    /** 专家咨询权限 */
    @Builder.Default
    private Boolean canAskExpert = false;

    /** 查看预警权限 */
    @Builder.Default
    private Boolean canViewAlerts = true;

    /** 查看历史数据权限 */
    @Builder.Default
    private Boolean canViewHistory = true;
}
