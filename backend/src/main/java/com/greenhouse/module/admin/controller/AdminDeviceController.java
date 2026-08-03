package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.admin.service.AdminDeviceService;
import com.greenhouse.module.greenhouse.service.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理员设备管理 API（R4）
 * <p>
 * 路径前缀：/api/v1/admin/devices，仅 ADMIN 角色可访问（SecurityConfig 已配置 /api/v1/admin/**）。
 * 提供：设备总体统计（按地区）、地区范围内棚主列表、某棚主名下全部设备（跨大棚）。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/devices")
@RequiredArgsConstructor
public class AdminDeviceController {

    private final AdminDeviceService adminDeviceService;
    private final RegionService regionService;

    /**
     * 设备总体统计（按地区范围）
     * GET /api/v1/admin/devices/overview?province=&city=&district=&town=&village=
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String village) {
        return ApiResponse.success(adminDeviceService.getOverview(province, city, district, town, village));
    }

    /**
     * 地区范围内的棚主列表（支持关键词搜索）
     * GET /api/v1/admin/devices/owners?province=&city=&district=&town=&village=&keyword=
     */
    @GetMapping("/owners")
    public ApiResponse<List<Map<String, Object>>> owners(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(regionService.getRegionOwners(province, city, district, town, village, keyword));
    }

    /**
     * 某棚主名下全部设备（按大棚分组）
     * GET /api/v1/admin/devices/owners/{ownerId}/devices
     */
    @GetMapping("/owners/{ownerId}/devices")
    public ApiResponse<List<Map<String, Object>>> ownerDevices(@PathVariable Long ownerId) {
        return ApiResponse.success(adminDeviceService.getOwnerDevices(ownerId));
    }
}