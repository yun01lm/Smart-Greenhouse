package com.greenhouse.module.permission.dto;

import com.greenhouse.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加员工请求 DTO（R23 重构）
 * <p>
 * 支持两种模式：
 * 1. 邀请模式：填写 identifier（已存在账号的用户名/手机号），绑定到当前棚主；
 * 2. 创建模式：填写 username/realName/phone/password，棚主直接创建员工账号。
 * 权限字段为空时按角色默认值填充（WORKER：看数据+控设备+看预警；TECHNICIAN：全部权限）。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddEmployeeRequest {

    /** 邀请模式：员工的用户名或手机号（账号已存在时使用） */
    private String identifier;

    /** 创建模式：用户名 */
    private String username;

    /** 创建模式：真实姓名 */
    private String realName;

    /** 创建模式：手机号 */
    private String phone;

    /** 创建模式：初始密码（>=8位，含字母和数字） */
    private String password;

    /** 员工类型：WORKER 普通员工 / TECHNICIAN 技术员，默认 WORKER */
    @Builder.Default
    private User.Role roleType = User.Role.WORKER;

    /** 授权的大棚ID（可空：仅创建账号，随后再分配大棚） */
    private Long greenhouseId;

    /** 查看数据权限（空则按角色默认值） */
    private Boolean canViewData;

    /** 控制设备权限 */
    private Boolean canControlDevice;

    /** 病虫害诊断权限 */
    private Boolean canDiagnose;

    /** 专家咨询权限 */
    private Boolean canAskExpert;

    /** 查看预警权限 */
    private Boolean canViewAlerts;

    /** 查看历史数据权限 */
    private Boolean canViewHistory;
}
