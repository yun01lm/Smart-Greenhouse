package com.greenhouse.module.device.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.Device;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.module.device.dto.DeviceRequest;
import com.greenhouse.module.device.dto.DeviceResponse;
import com.greenhouse.repository.DeviceRepository;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备管理服务
 * <p>
 * 负责设备的增删改查，以及状态追踪。
 * 权限规则：
 * - OWNER：只能操作自己大棚下的设备
 * - WORKER：只能查看被授权大棚下的设备（C18模块实现后完善）
 * - ADMIN：可查看所有设备
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final UserRepository userRepository;

    /** 每个大棚最多设备数量 */
    private static final long MAX_DEVICES_PER_GREENHOUSE = 50;

    /**
     * 创建设备
     */
    @Transactional
    public DeviceResponse createDevice(Long userId, Long greenhouseId, DeviceRequest request) {
        // 校验大棚存在且归属
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        // 只有棚主可以添加设备到自己的大棚
        if (user.getRole() != User.Role.ADMIN && !greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 校验设备数量上限
        long count = deviceRepository.countByGreenhouseId(greenhouseId);
        if (count >= MAX_DEVICES_PER_GREENHOUSE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "大棚设备数量已达上限(" + MAX_DEVICES_PER_GREENHOUSE + "个)");
        }

        // 校验设备编号唯一性
        if (deviceRepository.existsByGreenhouseIdAndDeviceSn(greenhouseId, request.getDeviceSn())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备编号已存在");
        }

        // 校验名称唯一性
        if (deviceRepository.existsByGreenhouseIdAndName(greenhouseId, request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备名称已存在");
        }

        // 校验传感器类型（传感器类设备必填）
        if (request.getDeviceType() == Device.DeviceType.SENSOR && request.getSensorType() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "传感器类设备必须指定传感器类型");
        }

        Device device = Device.builder()
                .name(request.getName())
                .deviceSn(request.getDeviceSn())
                .deviceType(request.getDeviceType())
                .sensorType(request.getSensorType())
                .greenhouseId(greenhouseId)
                .installLocation(request.getInstallLocation())
                .description(request.getDescription())
                .status(Device.DeviceStatus.OFFLINE)
                .build();

        device = deviceRepository.save(device);
        log.info("设备创建成功: id={}, name={}, sn={}, greenhouseId={}",
                device.getId(), device.getName(), device.getDeviceSn(), greenhouseId);

        return DeviceResponse.fromEntity(device);
    }

    /**
     * 获取大棚设备列表（按角色过滤）
     */
    public List<DeviceResponse> listDevices(Long userId, User.Role role, Long greenhouseId) {
        // 校验大棚存在
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        // 权限校验
        checkGreenhouseAccess(userId, role, greenhouse);

        List<Device> devices = deviceRepository.findByGreenhouseId(greenhouseId);
        return devices.stream()
                .map(DeviceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 按设备类型筛选
     */
    public List<DeviceResponse> listDevicesByType(Long userId, User.Role role,
                                                   Long greenhouseId, Device.DeviceType deviceType) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        checkGreenhouseAccess(userId, role, greenhouse);

        List<Device> devices = deviceRepository.findByGreenhouseIdAndDeviceType(greenhouseId, deviceType);
        return devices.stream()
                .map(DeviceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 按状态筛选
     */
    public List<DeviceResponse> listDevicesByStatus(Long userId, User.Role role,
                                                     Long greenhouseId, Device.DeviceStatus status) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        checkGreenhouseAccess(userId, role, greenhouse);

        List<Device> devices = deviceRepository.findByGreenhouseIdAndStatus(greenhouseId, status);
        return devices.stream()
                .map(DeviceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取设备详情
     */
    public DeviceResponse getDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        return DeviceResponse.fromEntity(device);
    }

    /**
     * 更新设备
     */
    @Transactional
    public DeviceResponse updateDevice(Long userId, Long deviceId, DeviceRequest request) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        // 校验大棚归属
        Greenhouse greenhouse = greenhouseRepository.findById(device.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        // 校验名称唯一性（排除自己）
        if (!device.getName().equals(request.getName())
                && deviceRepository.existsByGreenhouseIdAndName(device.getGreenhouseId(), request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备名称已存在");
        }

        device.setName(request.getName());
        device.setSensorType(request.getSensorType());
        device.setInstallLocation(request.getInstallLocation());
        device.setDescription(request.getDescription());
        // 注意：deviceSn 和 deviceType 创建后不可修改（硬件标识）

        device = deviceRepository.save(device);
        log.info("设备更新成功: id={}, name={}", device.getId(), device.getName());

        return DeviceResponse.fromEntity(device);
    }

    /**
     * 删除设备
     */
    @Transactional
    public void deleteDevice(Long userId, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        // 校验大棚归属
        Greenhouse greenhouse = greenhouseRepository.findById(device.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        deviceRepository.delete(device);
        log.info("设备删除成功: id={}, name={}, sn={}", device.getId(), device.getName(), device.getDeviceSn());
    }

    // ===== 辅助方法 =====

    /**
     * 校验用户是否有权限访问该大棚
     */
    private void checkGreenhouseAccess(Long userId, User.Role role, Greenhouse greenhouse) {
        switch (role) {
            case ADMIN:
                return; // 管理员可以看任何大棚
            case OWNER:
                if (!greenhouse.getOwnerId().equals(userId)) {
                    throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
                }
                return;
            case WORKER:
                // TODO: 步骤7（C18权限模块）实现后，检查员工是否被授权访问该大棚
                throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
            default:
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
