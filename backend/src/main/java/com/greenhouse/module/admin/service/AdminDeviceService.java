package com.greenhouse.module.admin.service;

import com.greenhouse.entity.Device;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.module.device.dto.DeviceResponse;
import com.greenhouse.module.greenhouse.service.RegionService;
import com.greenhouse.repository.DeviceRepository;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员设备管理服务（R4）
 * <p>
 * 面向系统管理员的设备管理视图：
 * - 按地区范围聚合设备总体统计（设备总数/在线/离线/告警、农户总数/在线）
 * - 查看某棚主名下全部大棚的设备（跨大棚，按大棚分组返回）
 * 农户在线口径：该农户名下至少 1 个设备在线（ONLINE 或 ALARM）即视为在线。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDeviceService {

    private final RegionService regionService;
    private final DeviceRepository deviceRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final UserRepository userRepository;

    /**
     * 地区范围内的设备总体统计
     */
    public Map<String, Object> getOverview(String province, String city, String district,
                                           String town, String village) {
        List<Greenhouse> ghs = regionService.getGreenhousesByRegion(province, city, district, town, village);
        List<Long> ghIds = ghs.stream().map(Greenhouse::getId).toList();

        long greenhouseCount = ghs.size();
        long ownerCount = ghs.stream().map(Greenhouse::getOwnerId).distinct().count();

        long deviceTotal = 0;
        long deviceOnline = 0;
        long deviceOffline = 0;
        long deviceAlarm = 0;
        long ownerOnline = 0;

        if (!ghIds.isEmpty()) {
            List<Device> devices = deviceRepository.findByGreenhouseIdIn(ghIds);
            deviceTotal = devices.size();
            Map<Long, Long> ghOwner = ghs.stream()
                    .collect(Collectors.toMap(Greenhouse::getId, Greenhouse::getOwnerId, (a, b) -> a));
            Set<Long> onlineOwners = new HashSet<>();
            for (Device d : devices) {
                Long ownerId = ghOwner.get(d.getGreenhouseId());
                switch (d.getStatus()) {
                    case ONLINE:
                        deviceOnline++;
                        if (ownerId != null) {
                            onlineOwners.add(ownerId);
                        }
                        break;
                    case OFFLINE:
                        deviceOffline++;
                        break;
                    case ALARM:
                        deviceAlarm++;
                        if (ownerId != null) {
                            onlineOwners.add(ownerId);
                        }
                        break;
                    default:
                        break;
                }
            }
            ownerOnline = onlineOwners.size();
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("greenhouseCount", greenhouseCount);
        stats.put("ownerCount", ownerCount);
        stats.put("ownerOnline", ownerOnline);
        stats.put("deviceTotal", deviceTotal);
        stats.put("deviceOnline", deviceOnline);
        stats.put("deviceOffline", deviceOffline);
        stats.put("deviceAlarm", deviceAlarm);
        return stats;
    }

    /**
     * 某棚主名下全部设备（按大棚分组）
     * 返回结构：[{ greenhouseId, greenhouseName, location, deviceCount, devices: [DeviceResponse] }]
     */
    public List<Map<String, Object>> getOwnerDevices(Long ownerId) {
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null || owner.getRole() != User.Role.OWNER) {
            return Collections.emptyList();
        }

        List<Greenhouse> ghs = greenhouseRepository.findByOwnerId(ownerId);
        if (ghs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ghIds = ghs.stream().map(Greenhouse::getId).toList();
        Map<Long, List<Device>> devicesByGh = deviceRepository.findByGreenhouseIdIn(ghIds).stream()
                .collect(Collectors.groupingBy(Device::getGreenhouseId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Greenhouse gh : ghs) {
            List<Device> devices = devicesByGh.getOrDefault(gh.getId(), Collections.emptyList());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("greenhouseId", gh.getId());
            item.put("greenhouseName", gh.getName());
            item.put("location", gh.getLocation() != null ? gh.getLocation() : "");
            item.put("deviceCount", devices.size());
            item.put("devices", devices.stream().map(DeviceResponse::fromEntity).toList());
            result.add(item);
        }
        return result;
    }
}