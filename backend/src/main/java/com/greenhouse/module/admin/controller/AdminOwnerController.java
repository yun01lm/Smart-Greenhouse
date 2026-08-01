package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 管理员棚主管理 API
 * <p>
 * 路径前缀：/api/v1/admin/owners，仅 ADMIN 角色可访问。
 * 提供棚主列表（聚合大棚数和员工数）、查看棚主名下大棚详情。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/owners")
@RequiredArgsConstructor
public class AdminOwnerController {

    private final UserRepository userRepository;
    private final GreenhouseRepository greenhouseRepository;

    /**
     * 棚主列表（含大棚数和员工数）
     * GET /api/v1/admin/owners
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listOwners() {
        List<User> owners = userRepository.findByRole(User.Role.OWNER);
        List<Map<String, Object>> result = new ArrayList<>();

        for (User owner : owners) {
            long greenhouseCount = greenhouseRepository.countByOwnerId(owner.getId());
            long employeeCount = userRepository.countByRoleAndOwnerId(User.Role.WORKER, owner.getId());

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", owner.getId());
            map.put("username", owner.getUsername());
            map.put("realName", owner.getRealName() != null ? owner.getRealName() : "");
            map.put("phone", owner.getPhone() != null ? owner.getPhone() : "");
            map.put("status", owner.getStatus());
            map.put("greenhouseCount", greenhouseCount);
            map.put("employeeCount", employeeCount);
            map.put("createdAt", owner.getCreatedAt());
            result.add(map);
        }
        return ApiResponse.success(result);
    }

    /**
     * 查看棚主名下大棚详情
     * GET /api/v1/admin/owners/{id}/greenhouses
     */
    @GetMapping("/{id}/greenhouses")
    public ApiResponse<List<Map<String, Object>>> listOwnerGreenhouses(
            @org.springframework.web.bind.annotation.PathVariable Long id) {

        User owner = userRepository.findById(id).orElse(null);
        if (owner == null || owner.getRole() != User.Role.OWNER) {
            return ApiResponse.success(Collections.emptyList());
        }

        List<Greenhouse> greenhouses = greenhouseRepository.findByOwnerId(id);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Greenhouse gh : greenhouses) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", gh.getId());
            map.put("name", gh.getName());
            map.put("location", gh.getLocation());
            map.put("cropType", gh.getCropType() != null ? gh.getCropType() : "");
            map.put("province", gh.getProvince() != null ? gh.getProvince() : "");
            map.put("city", gh.getCity() != null ? gh.getCity() : "");
            map.put("district", gh.getDistrict() != null ? gh.getDistrict() : "");
            map.put("town", gh.getTown() != null ? gh.getTown() : "");
            map.put("village", gh.getVillage() != null ? gh.getVillage() : "");
            map.put("status", gh.getStatus());
            map.put("createdAt", gh.getCreatedAt());
            result.add(map);
        }
        return ApiResponse.success(result);
    }
}
