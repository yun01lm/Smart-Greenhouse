package com.greenhouse.module.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.*;
import com.greenhouse.module.control.dto.ControlLogResponse;
import com.greenhouse.module.control.dto.ControlRequest;
import com.greenhouse.module.mqtt.MqttTopicConstants;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备控制服务
 * <p>
 * 负责通过 MQTT 向 ESP32 设备下发控制指令，并记录控制日志。
 * 权限规则：
 * - OWNER：只能控制自己大棚下的设备
 * - WORKER：需要 canControlDevice 权限
 * - ADMIN：可控制所有设备
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ControlService {

    private final MqttClient mqttClient;
    private final DeviceRepository deviceRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final ControlLogRepository controlLogRepository;
    private final SceneRepository sceneRepository;
    private final UserRepository userRepository;
    private final EmployeePermissionRepository permissionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 控制单个设备
     *
     * @param userId  操作人ID
     * @param role    操作人角色
     * @param request 控制请求（设备ID + 动作）
     * @return 控制日志
     */
    @Transactional
    public ControlLogResponse controlDevice(Long userId, User.Role role, ControlRequest request) {
        // 1. 校验设备存在
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        // 2. 校验设备类型必须是 CONTROLLER
        if (device.getDeviceType() != Device.DeviceType.CONTROLLER) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有控制器类设备才能执行操作");
        }

        // 3. 校验设备在线
        if (device.getStatus() == Device.DeviceStatus.OFFLINE) {
            throw new BusinessException(ErrorCode.DEVICE_OFFLINE);
        }

        // 4. 校验大棚存在 + 权限
        Greenhouse greenhouse = greenhouseRepository.findById(device.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        checkControlPermission(userId, role, greenhouse, request.getDeviceId());

        // 5. 校验动作合法性
        String action = request.getAction().toUpperCase();
        if (!"ON".equals(action) && !"OFF".equals(action)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "控制动作只能是 ON 或 OFF");
        }

        // 6. 通过 MQTT 下发控制指令
        boolean success = sendMqttCommand(device, action);
        String failReason = success ? null : "MQTT 发送失败，设备可能不在线";

        // 7. 记录控制日志
        ControlLog controlLog = ControlLog.builder()
                .userId(userId)
                .deviceId(request.getDeviceId())
                .action(action)
                .source("MANUAL")
                .success(success)
                .failReason(failReason)
                .build();
        controlLog = controlLogRepository.save(controlLog);

        // 8. 更新设备状态
        if (success) {
            device.setLastValue(action);
            device.setLastDataTime(java.time.LocalDateTime.now());
            deviceRepository.save(device);
        }

        log.info("设备控制: userId={}, deviceId={}, action={}, success={}", userId, device.getId(), action, success);

        String username = userRepository.findById(userId)
                .map(User::getUsername)
                .orElse("未知用户");

        return ControlLogResponse.fromEntity(controlLog, username, device.getName());
    }

    /**
     * 系统自动控制设备（预警联动触发）
     * <p>
     * 跳过用户权限校验（系统操作），操作人记为空（展示为"系统"），
     * 日志来源固定为 ALERT 并记录触发场景ID。
     * </p>
     *
     * @param deviceId 设备ID
     * @param action   动作（ON / OFF）
     * @param sceneId  触发场景ID（预警关联的场景）
     */
    @Transactional
    public ControlLogResponse controlDeviceBySystem(Long deviceId, String action, Long sceneId) {
        // 1. 校验设备存在
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));

        // 2. 校验设备类型必须是 CONTROLLER
        if (device.getDeviceType() != Device.DeviceType.CONTROLLER) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有控制器类设备才能执行操作");
        }

        // 3. 校验设备在线
        if (device.getStatus() == Device.DeviceStatus.OFFLINE) {
            throw new BusinessException(ErrorCode.DEVICE_OFFLINE);
        }

        // 4. 校验动作合法性
        String act = action.toUpperCase();
        if (!"ON".equals(act) && !"OFF".equals(act)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "控制动作只能是 ON 或 OFF");
        }

        // 5. 通过 MQTT 下发控制指令
        boolean success = sendMqttCommand(device, act);
        String failReason = success ? null : "MQTT 发送失败，设备可能不在线";

        // 6. 记录控制日志（系统触发：userId=null, source=ALERT, sceneId）
        ControlLog controlLog = ControlLog.builder()
                .userId(null)
                .deviceId(deviceId)
                .action(act)
                .source("ALERT")
                .sceneId(sceneId)
                .success(success)
                .failReason(failReason)
                .build();
        controlLog = controlLogRepository.save(controlLog);

        // 7. 更新设备状态
        if (success) {
            device.setLastValue(act);
            device.setLastDataTime(java.time.LocalDateTime.now());
            deviceRepository.save(device);
        }

        log.info("系统预警联动设备控制: deviceId={}, action={}, sceneId={}, success={}",
                deviceId, act, sceneId, success);

        return ControlLogResponse.fromEntity(controlLog, "系统", device.getName());
    }

    /**
     * 查询设备的控制日志
     */
    public List<ControlLogResponse> getDeviceLogs(Long deviceId) {
        List<ControlLog> logs = controlLogRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId);

        return logs.stream()
                .map(this::toControlLogResponse)
                .collect(Collectors.toList());
    }

    /**
     * 按大棚查询控制日志（分页，用户权限收口）
     * <p>
     * OWNER 只能查自己大棚；WORKER/TECHNICIAN 需有该大棚授权；ADMIN 可查任意大棚。
     * 可通过 source 过滤来源（MANUAL / SCENE / ALERT）。
     * </p>
     */
    public Page<ControlLogResponse> getGreenhouseLogs(Long userId, User.Role role, Long greenhouseId,
                                                      String source, int page, int size) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        switch (role) {
            case ADMIN:
                break;
            case OWNER:
                if (!greenhouse.getOwnerId().equals(userId)) {
                    throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
                }
                break;
            case WORKER:
            case TECHNICIAN:
                var perm = permissionRepository.findByEmployeeIdAndGreenhouseId(userId, greenhouseId);
                if (perm.isEmpty()) {
                    throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
                }
                break;
            default:
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        List<Long> deviceIds = deviceRepository.findByGreenhouseId(greenhouseId).stream()
                .map(Device::getId)
                .collect(Collectors.toList());
        if (deviceIds.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ControlLog> logPage;
        if (source != null && !source.isBlank()) {
            logPage = controlLogRepository.findByDeviceIdInAndSourceOrderByCreatedAtDesc(
                    deviceIds, source.toUpperCase(), pageable);
        } else {
            logPage = controlLogRepository.findByDeviceIdInOrderByCreatedAtDesc(deviceIds, pageable);
        }
        return logPage.map(this::toControlLogResponse);
    }

    /**
     * 控制日志转响应 DTO（操作人：系统触发显示"系统"；附带触发场景名称）
     */
    private ControlLogResponse toControlLogResponse(ControlLog log) {
        String username = log.getUserId() != null
                ? userRepository.findById(log.getUserId()).map(User::getUsername).orElse("未知用户")
                : "系统";
        String deviceName = deviceRepository.findById(log.getDeviceId())
                .map(Device::getName).orElse("未知设备");
        String sceneName = null;
        if (log.getSceneId() != null) {
            sceneName = sceneRepository.findById(log.getSceneId())
                    .map(Scene::getName).orElse(null);
        }
        ControlLogResponse response = ControlLogResponse.fromEntity(log, username, deviceName);
        response.setSceneName(sceneName);
        return response;
    }

    // ===== 辅助方法 =====

    /**
     * 通过 MQTT 向设备下发控制指令
     * <p>
     * 新固件 Topic: device/{firmwareId}/command，Payload: {"action":"ON","timestamp":1753088400000}
     * 旧格式回退: greenhouse/{greenhouseId}/device/{deviceSn}/command（存量设备兼容）
     * </p>
     */
    private boolean sendMqttCommand(Device device, String action) {
        try {
            String topic;
            if (device.getFirmwareId() != null) {
                topic = MqttTopicConstants.firmwareControlTopic(device.getFirmwareId());
            } else {
                topic = MqttTopicConstants.deviceControlTopic(
                        device.getGreenhouseId(), device.getDeviceSn());
            }

            Map<String, Object> payload = Map.of(
                    "action", action,
                    "timestamp", System.currentTimeMillis()
            );
            String jsonPayload = objectMapper.writeValueAsString(payload);

            MqttMessage message = new MqttMessage(jsonPayload.getBytes());
            message.setQos(1);
            message.setRetained(false);

            mqttClient.publish(topic, message);
            log.debug("MQTT 控制指令已发送: topic={}, payload={}", topic, jsonPayload);
            return true;
        } catch (Exception e) {
            log.error("MQTT 控制指令发送失败: deviceId={}, action={}, error={}",
                    device.getId(), action, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 校验设备控制权限
     */
    void checkControlPermission(Long userId, User.Role role, Greenhouse greenhouse, Long deviceId) {
        switch (role) {
            case ADMIN:
                return;
            case OWNER:
                if (!greenhouse.getOwnerId().equals(userId)) {
                    throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
                }
                return;
            case WORKER:
                var perm = permissionRepository.findByEmployeeIdAndGreenhouseId(userId, greenhouse.getId());
                if (perm.isEmpty() || !perm.get().getCanControlDevice()) {
                    throw new BusinessException(ErrorCode.FUNCTION_DENIED);
                }
                return;
            case TECHNICIAN:
                var techPerm = permissionRepository.findByEmployeeIdAndGreenhouseId(userId, greenhouse.getId());
                if (techPerm.isEmpty() || !techPerm.get().getCanControlDevice()) {
                    throw new BusinessException(ErrorCode.FUNCTION_DENIED);
                }
                return;
            default:
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
