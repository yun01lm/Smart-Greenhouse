package com.greenhouse.module.device.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.Device;
import com.greenhouse.entity.DeviceGroup;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.module.device.dto.DeviceGroupRequest;
import com.greenhouse.module.device.dto.DeviceGroupResponse;
import com.greenhouse.repository.DeviceGroupRepository;
import com.greenhouse.repository.DeviceRepository;
import com.greenhouse.repository.GreenhouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备分组管理服务
 * <p>
 * 负责设备分组的增删改查，以及组内设备的添加/移除。
 * 权限规则与设备管理一致。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceGroupService {

    private final DeviceGroupRepository groupRepository;
    private final DeviceRepository deviceRepository;
    private final GreenhouseRepository greenhouseRepository;

    /** 每个大棚最多分组数量 */
    private static final long MAX_GROUPS_PER_GREENHOUSE = 20;

    /**
     * 创建分组
     */
    @Transactional
    public DeviceGroupResponse createGroup(Long userId, Long greenhouseId, DeviceGroupRequest request) {
        // 校验大棚存在且归属
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 校验分组数量上限
        long count = groupRepository.countByGreenhouseId(greenhouseId);
        if (count >= MAX_GROUPS_PER_GREENHOUSE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "大棚分组数量已达上限(" + MAX_GROUPS_PER_GREENHOUSE + "个)");
        }

        // 校验名称唯一性
        if (groupRepository.existsByGreenhouseIdAndName(greenhouseId, request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分组名称已存在");
        }

        // 校验设备ID合法性（确保设备属于同一大棚）
        List<Long> deviceIds = request.getDeviceIds() != null ? request.getDeviceIds() : List.of();
        if (!deviceIds.isEmpty()) {
            List<Device> devices = deviceRepository.findByIdIn(deviceIds);
            for (Device device : devices) {
                if (!device.getGreenhouseId().equals(greenhouseId)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "设备「" + device.getName() + "」不属于该大棚");
                }
            }
        }

        DeviceGroup group = DeviceGroup.builder()
                .name(request.getName())
                .greenhouseId(greenhouseId)
                .description(request.getDescription())
                .deviceIds(new ArrayList<>(deviceIds))
                .build();

        group = groupRepository.save(group);
        log.info("设备分组创建成功: id={}, name={}, greenhouseId={}, deviceCount={}",
                group.getId(), group.getName(), greenhouseId, deviceIds.size());

        return DeviceGroupResponse.fromEntity(group);
    }

    /**
     * 获取大棚分组列表
     */
    public List<DeviceGroupResponse> listGroups(Long greenhouseId) {
        // 校验大棚存在
        if (!greenhouseRepository.existsById(greenhouseId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND);
        }

        List<DeviceGroup> groups = groupRepository.findByGreenhouseId(greenhouseId);
        return groups.stream()
                .map(DeviceGroupResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取分组详情
     */
    public DeviceGroupResponse getGroup(Long groupId) {
        DeviceGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_GROUP_NOT_FOUND));
        return DeviceGroupResponse.fromEntity(group);
    }

    /**
     * 更新分组（修改名称、描述、设备列表）
     */
    @Transactional
    public DeviceGroupResponse updateGroup(Long userId, Long groupId, DeviceGroupRequest request) {
        DeviceGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_GROUP_NOT_FOUND));

        // 校验大棚归属
        Greenhouse greenhouse = greenhouseRepository.findById(group.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 校验名称唯一性（排除自己）
        if (!group.getName().equals(request.getName())
                && groupRepository.existsByGreenhouseIdAndName(group.getGreenhouseId(), request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分组名称已存在");
        }

        // 校验设备ID合法性
        List<Long> deviceIds = request.getDeviceIds() != null ? request.getDeviceIds() : List.of();
        if (!deviceIds.isEmpty()) {
            List<Device> devices = deviceRepository.findByIdIn(deviceIds);
            for (Device device : devices) {
                if (!device.getGreenhouseId().equals(group.getGreenhouseId())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "设备「" + device.getName() + "」不属于该大棚");
                }
            }
        }

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setDeviceIds(new ArrayList<>(deviceIds));

        group = groupRepository.save(group);
        log.info("设备分组更新成功: id={}, name={}, deviceCount={}",
                group.getId(), group.getName(), deviceIds.size());

        return DeviceGroupResponse.fromEntity(group);
    }

    /**
     * 向分组添加设备
     */
    @Transactional
    public DeviceGroupResponse addDeviceToGroup(Long userId, Long groupId, Long deviceId) {
        DeviceGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_GROUP_NOT_FOUND));

        // 校验权限
        Greenhouse greenhouse = greenhouseRepository.findById(group.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 校验设备存在且属于同一大棚
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        if (!device.getGreenhouseId().equals(group.getGreenhouseId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备不属于该大棚");
        }

        if (!group.getDeviceIds().contains(deviceId)) {
            group.getDeviceIds().add(deviceId);
            group = groupRepository.save(group);
            log.info("设备加入分组: deviceId={}, groupId={}", deviceId, groupId);
        }

        return DeviceGroupResponse.fromEntity(group);
    }

    /**
     * 从分组移除设备
     */
    @Transactional
    public DeviceGroupResponse removeDeviceFromGroup(Long userId, Long groupId, Long deviceId) {
        DeviceGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_GROUP_NOT_FOUND));

        // 校验权限
        Greenhouse greenhouse = greenhouseRepository.findById(group.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        group.getDeviceIds().remove(deviceId);
        group = groupRepository.save(group);
        log.info("设备移出分组: deviceId={}, groupId={}", deviceId, groupId);

        return DeviceGroupResponse.fromEntity(group);
    }

    /**
     * 删除分组
     */
    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        DeviceGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_GROUP_NOT_FOUND));

        // 校验权限
        Greenhouse greenhouse = greenhouseRepository.findById(group.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        groupRepository.delete(group);
        log.info("设备分组删除成功: id={}, name={}", group.getId(), group.getName());
    }
}
