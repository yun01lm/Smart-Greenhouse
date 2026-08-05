package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.PageResult;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员棚主管理 API
 * <p>
 * 路径前缀：/api/v1/admin/owners，仅 ADMIN 角色可访问。
 * 提供棚主列表（关键词搜索 + 五级地区筛选 + 分页）、查看棚主名下大棚详情。
 * R10：新增 keyword/地区筛选/分页/regionText，支撑棚主管理的搜索与精准定位。
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
     * 棚主列表（含大棚数/员工数/地区，支持关键词 + 地区筛选 + 分页）
     * GET /api/v1/admin/owners?keyword=&province=&city=&district=&town=&village=&page=0&size=20
     */
    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> listOwners(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String town,
            @RequestParam(required = false) String village,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<User> owners = userRepository.findByRole(User.Role.OWNER);
        List<Map<String, Object>> result = new ArrayList<>();

        for (User owner : owners) {
            List<Greenhouse> ghs = greenhouseRepository.findByOwnerId(owner.getId());

            // 关键词过滤：用户名 / 姓名 / 手机号（忽略大小写）
            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.trim().toLowerCase();
                boolean hit = owner.getUsername().toLowerCase().contains(kw)
                        || (owner.getRealName() != null && owner.getRealName().toLowerCase().contains(kw))
                        || (owner.getPhone() != null && owner.getPhone().contains(kw));
                if (!hit) continue;
            }

            // 地区过滤：棚主名下存在一个大棚命中指定层级（省→市→县→乡镇→村逐级匹配）
            if (!matchRegion(ghs, province, city, district, town, village)) continue;

            long greenhouseCount = ghs.size();
            long employeeCount = userRepository.countByRoleAndOwnerId(User.Role.WORKER, owner.getId());

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", owner.getId());
            map.put("username", owner.getUsername());
            map.put("realName", owner.getRealName() != null ? owner.getRealName() : "");
            map.put("phone", owner.getPhone() != null ? owner.getPhone() : "");
            map.put("status", owner.getStatus());
            map.put("greenhouseCount", greenhouseCount);
            map.put("employeeCount", employeeCount);
            map.put("regionText", buildRegionText(ghs));
            map.put("createdAt", owner.getCreatedAt());
            result.add(map);
        }

        // 内存分页
        int from = Math.min(page * size, result.size());
        int to = Math.min(from + size, result.size());
        List<Map<String, Object>> pageList = from < to ? result.subList(from, to) : Collections.emptyList();
        return ApiResponse.success(PageResult.of(pageList, result.size(), page, size));
    }

    /**
     * 查看棚主名下大棚详情
     * GET /api/v1/admin/owners/{id}/greenhouses
     */
    @GetMapping("/{id}/greenhouses")
    public ApiResponse<List<Map<String, Object>>> listOwnerGreenhouses(
            @PathVariable Long id) {

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

    // ===== 辅助 =====

    /** 地区逐级匹配：仅当指定层级全部命中同一大棚时返回 true */
    private boolean matchRegion(List<Greenhouse> ghs, String province, String city,
                                String district, String town, String village) {
        if ((province == null || province.isBlank())
                && (city == null || city.isBlank())
                && (district == null || district.isBlank())
                && (town == null || town.isBlank())
                && (village == null || village.isBlank())) {
            return true;
        }
        if (ghs.isEmpty()) return false;
        for (Greenhouse gh : ghs) {
            if (province != null && !province.isBlank() && !Objects.equals(gh.getProvince(), province)) continue;
            if (city != null && !city.isBlank() && !Objects.equals(gh.getCity(), city)) continue;
            if (district != null && !district.isBlank() && !Objects.equals(gh.getDistrict(), district)) continue;
            if (town != null && !town.isBlank() && !Objects.equals(gh.getTown(), town)) continue;
            if (village != null && !village.isBlank() && !Objects.equals(gh.getVillage(), village)) continue;
            return true;
        }
        return false;
    }

    /** 聚合棚主地区文本（取第一个大棚的省/市/县/乡镇/村，用 / 连接） */
    private String buildRegionText(List<Greenhouse> ghs) {
        if (ghs.isEmpty()) return "";
        Greenhouse gh = ghs.get(0);
        List<String> parts = new ArrayList<>();
        if (gh.getProvince() != null && !gh.getProvince().isBlank()) parts.add(gh.getProvince());
        if (gh.getCity() != null && !gh.getCity().isBlank()) parts.add(gh.getCity());
        if (gh.getDistrict() != null && !gh.getDistrict().isBlank()) parts.add(gh.getDistrict());
        if (gh.getTown() != null && !gh.getTown().isBlank()) parts.add(gh.getTown());
        if (gh.getVillage() != null && !gh.getVillage().isBlank()) parts.add(gh.getVillage());
        return String.join(" / ", parts);
    }
}
