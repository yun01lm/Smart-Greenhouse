package com.greenhouse.module.greenhouse.service;

import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 地区服务（管理员功能）
 * <p>
 * 地区层级从大棚登记的省/市/县(区)/乡镇/村五级字段聚合而来（方案A）。
 * 设计上封装为独立服务，后续如需升级为标准行政区划表（方案B），仅替换本服务实现即可。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegionService {

    private final GreenhouseRepository greenhouseRepository;
    private final UserRepository userRepository;

    /** 省份列表 */
    public List<String> getProvinces() {
        return greenhouseRepository.findDistinctProvinces();
    }

    /** 城市列表 */
    public List<String> getCities(String province) {
        return greenhouseRepository.findDistinctCities(normalize(province));
    }

    /** 区县列表 */
    public List<String> getDistricts(String province, String city) {
        return greenhouseRepository.findDistinctDistricts(normalize(province), normalize(city));
    }

    /** 乡镇列表 */
    public List<String> getTowns(String province, String city, String district) {
        return greenhouseRepository.findDistinctTowns(normalize(province), normalize(city), normalize(district));
    }

    /** 村列表 */
    public List<String> getVillages(String province, String city, String district, String town) {
        return greenhouseRepository.findDistinctVillages(normalize(province), normalize(city), normalize(district), normalize(town));
    }

    /** 地区范围内的大棚 */
    public List<Greenhouse> getGreenhousesByRegion(String province, String city, String district, String town, String village) {
        return greenhouseRepository.findByRegion(
                normalize(province), normalize(city), normalize(district), normalize(town), normalize(village));
    }

    /**
     * 地区范围内的棚主用户（含大棚数，支持关键词搜索用户名/姓名/手机号）
     */
    public List<Map<String, Object>> getRegionOwners(String province, String city, String district,
                                                     String town, String village, String keyword) {
        List<Greenhouse> ghs = getGreenhousesByRegion(province, city, district, town, village);
        Map<Long, Long> greenhouseCount = ghs.stream()
                .collect(Collectors.groupingBy(Greenhouse::getOwnerId, Collectors.counting()));

        Set<Long> ownerIds = greenhouseCount.keySet();
        if (ownerIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> owners = userRepository.findAllById(ownerIds);
        String kw = keyword == null ? null : keyword.trim().toLowerCase();

        List<Map<String, Object>> result = new ArrayList<>();
        for (User owner : owners) {
            if (owner.getRole() != User.Role.OWNER) {
                continue;
            }
            if (kw != null && !kw.isEmpty()) {
                boolean match = (owner.getUsername() != null && owner.getUsername().toLowerCase().contains(kw))
                        || (owner.getRealName() != null && owner.getRealName().toLowerCase().contains(kw))
                        || (owner.getPhone() != null && owner.getPhone().toLowerCase().contains(kw));
                if (!match) {
                    continue;
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", owner.getId());
            item.put("username", owner.getUsername());
            item.put("realName", owner.getRealName());
            item.put("phone", owner.getPhone());
            item.put("status", owner.getStatus());
            item.put("greenhouseCount", greenhouseCount.getOrDefault(owner.getId(), 0L));
            result.add(item);
        }
        result.sort(Comparator.comparing(m -> String.valueOf(m.get("username"))));
        return result;
    }

    private String normalize(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
