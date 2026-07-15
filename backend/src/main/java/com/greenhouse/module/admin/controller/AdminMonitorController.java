package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.admin.dto.MonitorOverviewResponse;
import com.greenhouse.module.admin.service.AdminMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员系统监控 API
 * <p>
 * 路径前缀：/api/v1/admin/monitor，仅 ADMIN 角色可访问。
 * 提供系统运行状态综合概览，包含设备在线率、告警统计、服务连接状态、系统数据概览。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/monitor")
@RequiredArgsConstructor
public class AdminMonitorController {

    private final AdminMonitorService monitorService;

    /**
     * 获取系统监控综合概览
     * GET /api/v1/admin/monitor/overview
     */
    @GetMapping("/overview")
    public ApiResponse<MonitorOverviewResponse> getOverview() {
        log.info("[ADMIN] 查看系统监控概览");
        return ApiResponse.success(monitorService.getOverview());
    }
}
