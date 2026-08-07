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
     * 查询设备的控制日志
     */
    public List<ControlLogResponse> getDeviceLogs(Long deviceId) {
        List<ControlLog> logs = controlLogRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId);

        return logs.stream()
                .map(log -> {
                    String username = log.getUserId() != null
                            ? userRepository.findById(log.getUserId()).map(User::getUsername).orElse("未知用户")
                            : "系统";
                    String deviceName = deviceRepository.findById(log.getDeviceId())
                            .map(Device::getName).orElse("未知设备");
                    return ControlLogResponse.fromEntity(log, username, deviceName);
                })
                .collect(Collectors.toList());
    }

    // ===== 辅助方法 =====

    /**
     * 通过 MQTT 向设备下发控制指令
     * <p>
     * Topic: greenhouse/{greenhouseId}/device/{deviceSn}/command
     * Payload: {"action":"ON","timestamp":1753088400000}
     * </p>
     */
    private boolean sendMqttCommand(Device device, String action) {
        try {
            String topic = MqttTopicConstants.deviceControlTopic(
                    device.getGreenhouseId(), device.getDeviceSn());

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
