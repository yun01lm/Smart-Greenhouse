package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.greenhouse.service.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理员地区 API
 * <p>
 * 路径前缀：/api/v1/admin/regions，仅 ADMIN 角色可访问（SecurityConfig 已配置）。
 * 提供省/市/县(区)/乡镇/村五级地区聚合接口，以及地区范围内的棚主用户查询。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/regions")
@RequiredArgsConstructor
public class AdminRegionController {

    private final RegionService regionService;

    /** GET /api/v1/admin/regions/provinces */
    @GetMapping("/provinces")
    public ApiResponse<List<String>> provinces() {
        return ApiResponse.success(regionService.getProvinces());
    }

    /** GET /api/v1/admin/regions/cities?province= */
    @GetMapping("/cities")
    public ApiResponse<List<String>> cities(@RequestParam String province) {
        return ApiResponse.success(regionService.getCities(province));
    }

    /** GET /api/v1/admin/regions/districts?province=&city= */
    @GetMapping("/districts")
    public ApiResponse<List<String>> districts(@RequestParam String province, @RequestParam String city) {
        return ApiResponse.success(regionService.getDistricts(province, city));
    }

    /** GET /api/v1/admin/regions/towns?province=&city=&district= */
    @GetMapping("/towns")
    public ApiResponse<List<String>> towns(@RequestParam String province, @RequestParam String city,
                                           @RequestParam String district) {
        return ApiResponse.success(regionService.getTowns(province, city, district));
    }

    /** GET /api/v1/admin/regions/villages?province=&city=&district=&town= */
    @GetMapping("/villages")
    public ApiResponse<List<String>> villages(@RequestParam String province, @RequestParam String city,
                                              @RequestParam String district, @RequestParam String town) {
        return ApiResponse.success(regionService.getVillages(province, city, district, town));
    }

    /** GET /api/v1/admin/regions/users?province=&city=&district=&town=&village=&keyword= */
    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(regionService.getRegionOwners(province, city, district, town, village, keyword));
    }
}
