package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员数据总览 API（R3）
 * <p>
 * 路径前缀：/api/v1/admin/dashboard，仅 ADMIN 角色可访问。
 * 按地区范围返回：整体统计、环境聚合、预警总览、地区健康评分、最新预警、当前天气、系统监控。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    /** GET /api/v1/admin/dashboard/overview?province=&city=&district=&town=&village= */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String village) {
        return ApiResponse.success(dashboardService.getOverview(province, city, district, town, village));
    }
}
