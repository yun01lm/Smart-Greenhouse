package com.greenhouse.module.websocket.service;

import com.greenhouse.entity.Device;
import com.greenhouse.module.websocket.dto.AlertPushMessage;
import com.greenhouse.module.websocket.dto.DeviceStatusMessage;
import com.greenhouse.module.websocket.dto.RealtimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * WebSocket 实时推送服务
 * <p>
 * 封装 SimpMessagingTemplate，提供统一的推送入口。
 * 所有推送操作都通过此服务，便于统一管理和日志追踪。
 * </p>
 *
 * <h3>推送主题</h3>
 * <ul>
 *   <li>/topic/greenhouse/{greenhouseId}/realtime — 实时传感器数据</li>
 *   <li>/topic/greenhouse/{greenhouseId}/alerts — 预警推送</li>
 *   <li>/topic/device/{deviceId}/status — 设备状态变更</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimePushService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送实时传感器数据
     * <p>
     * 当 MQTT 收到传感器上报数据时调用。
     * </p>
     */
    public void pushSensorData(Long greenhouseId, Long deviceId, String deviceName,
                                String sensorType, Double value) {
        RealtimeMessage message = RealtimeMessage.builder()
                .type("SENSOR_DATA")
                .greenhouseId(greenhouseId)
                .deviceId(deviceId)
                .deviceName(deviceName)
                .sensorType(sensorType)
                .value(value)
                .timestamp(Instant.now())
                .build();

        String destination = "/topic/greenhouse/" + greenhouseId + "/realtime";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("WebSocket 推送实时数据: destination={}, sensorType={}, value={}",
                destination, sensorType, value);
    }

    /**
     * 推送设备状态变更
     * <p>
     * 当设备上线/离线/告警时调用。
     * </p>
     */
    public void pushDeviceStatus(Device device, String oldStatus, String newStatus) {
        DeviceStatusMessage message = DeviceStatusMessage.builder()
                .type("DEVICE_STATUS")
                .deviceId(device.getId())
                .deviceName(device.getName())
                .greenhouseId(device.getGreenhouseId())
                .status(newStatus)
                .lastValue(device.getLastValue())
                .timestamp(Instant.now())
                .build();

        String destination = "/topic/device/" + device.getId() + "/status";
        messagingTemplate.convertAndSend(destination, message);
        log.info("WebSocket 推送设备状态变更: deviceId={}, {} → {}",
                device.getId(), oldStatus, newStatus);
    }

    /**
     * 推送预警消息
     * <p>
     * 为步骤 10（C6 预警引擎）做准备。
     * </p>
     */
    public void pushAlert(Long alertId, Long greenhouseId, String level,
                           String title, String content, String sensorType,
                           Double currentValue, Double thresholdValue) {
        AlertPushMessage message = AlertPushMessage.builder()
                .type("ALERT")
                .alertId(alertId)
                .greenhouseId(greenhouseId)
                .level(level)
                .title(title)
                .content(content)
                .sensorType(sensorType)
                .currentValue(currentValue)
                .thresholdValue(thresholdValue)
                .timestamp(Instant.now())
                .build();

        String destination = "/topic/greenhouse/" + greenhouseId + "/alerts";
        messagingTemplate.convertAndSend(destination, message);
        log.info("WebSocket 推送预警: greenhouseId={}, level={}, title={}",
                greenhouseId, level, title);
    }
}
