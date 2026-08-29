package com.greenhouse.module.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.entity.Device;
import com.greenhouse.module.alert.service.AlertEngine;
import com.greenhouse.module.sensor.service.SensorDataService;
import com.greenhouse.module.websocket.service.RealtimePushService;
import com.greenhouse.repository.DeviceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

/**
 * MQTT 消息订阅器
 * <p>
 * 启动后自动订阅两类设备数据主题（通配符）：
 * <ul>
 *   <li>新格式 {@code device/+/data}：固件按固件ID（8位数字，出厂预注册）上报，
 *       后端反查设备定位大棚，未绑定固件丢弃并记日志；</li>
 *   <li>旧格式 {@code greenhouse/+/device/+}：兼容存量设备与模拟器（按 deviceId 解析）。</li>
 * </ul>
 * 接收 ESP32 设备上报的传感器数据并写入 InfluxDB。
 * </p>
 *
 * <h3>新固件上报数据格式（JSON）</h3>
 * <pre>{@code
 * {
 *   "firmwareId": "00000001",
 *   "sensorType": "TEMPERATURE",
 *   "value": 25.6,
 *   "timestamp": 1753088400000
 * }
 * }</pre>
 *
 * <h3>控制器心跳（新格式）</h3>
 * <pre>{@code
 * {
 *   "firmwareId": "00000002",
 *   "deviceType": "CONTROLLER",
 *   "timestamp": 1753088400000
 * }
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttSubscriber {

    private final MqttClient mqttClient;
    private final SensorDataService sensorDataService;
    private final RealtimePushService pushService;
    private final AlertEngine alertEngine;
    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;

    @PostConstruct
    public void init() {
        try {
            // 新格式：device/+/data（固件按固件ID上报）
            mqttClient.subscribe(MqttTopicConstants.FIRMWARE_DATA_WILDCARD,
                    MqttTopicConstants.DEFAULT_QOS, new SensorDataListener());
            // 旧格式：greenhouse/+/device/+（兼容存量设备与模拟器）
            mqttClient.subscribe(MqttTopicConstants.DEVICE_DATA_WILDCARD,
                    MqttTopicConstants.DEFAULT_QOS, new SensorDataListener());
            log.info("MQTT 订阅成功: 新格式={}, 旧格式={}",
                    MqttTopicConstants.FIRMWARE_DATA_WILDCARD, MqttTopicConstants.DEVICE_DATA_WILDCARD);
        } catch (MqttException e) {
            log.error("MQTT 订阅失败: error={}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.unsubscribe(MqttTopicConstants.FIRMWARE_DATA_WILDCARD);
                mqttClient.unsubscribe(MqttTopicConstants.DEVICE_DATA_WILDCARD);
                mqttClient.disconnect();
                log.info("MQTT 已断开连接");
            }
        } catch (MqttException e) {
            log.warn("MQTT 断开连接异常: {}", e.getMessage());
        }
    }

    /**
     * 传感器数据消息监听器
     */
    private class SensorDataListener implements IMqttMessageListener {

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            try {
                String payload = new String(message.getPayload());
                log.debug("MQTT 收到消息: topic={}, payload={}", topic, payload);

                JsonNode root = objectMapper.readTree(payload);

                if (topic.startsWith("device/") && topic.endsWith("/data")) {
                    // 新格式：固件按固件ID上报
                    handleFirmwareData(topic, root);
                } else {
                    // 旧格式：按 payload 中的 deviceId 解析（存量兼容）
                    handleLegacyData(topic, root);
                }
            } catch (Exception e) {
                log.error("MQTT 消息处理失败: topic={}, error={}", topic, e.getMessage(), e);
            }
        }

        /**
         * 新格式数据处理：按固件ID反查设备
         */
        private void handleFirmwareData(String topic, JsonNode root) {
            String firmwareId = root.get("firmwareId").asText();

            Device device = deviceRepository.findByFirmwareId(firmwareId).orElse(null);
            if (device == null) {
                // 固件未绑定设备：丢弃数据，记录日志（正常流程中不应出现）
                log.warn("固件未绑定设备，数据丢弃: firmwareId={}, topic={}", firmwareId, topic);
                return;
            }

            Long greenhouseId = device.getGreenhouseId();
            Long deviceId = device.getId();

            // 判断消息类型：CONTROLLER 心跳 vs 传感器数据
            String deviceType = root.has("deviceType") ? root.get("deviceType").asText() : null;
            if ("CONTROLLER".equals(deviceType)) {
                sensorDataService.updateDeviceOnline(deviceId);
                log.debug("控制器心跳已处理(新格式): greenhouseId={}, deviceId={}, firmwareId={}",
                        greenhouseId, deviceId, firmwareId);
                return;
            }

            processSensorData(greenhouseId, deviceId, device.getName(), root);
        }

        /**
         * 旧格式数据处理：按 payload 中的 deviceId 解析（存量兼容）
         */
        private void handleLegacyData(String topic, JsonNode root) {
            Long greenhouseId = root.get("greenhouseId").asLong();
            Long deviceId = root.get("deviceId").asLong();

            // 判断消息类型：CONTROLLER 心跳 vs 传感器数据
            String deviceType = root.has("deviceType") ? root.get("deviceType").asText() : null;
            if ("CONTROLLER".equals(deviceType)) {
                sensorDataService.updateDeviceOnline(deviceId);
                log.debug("控制器心跳已处理(旧格式): greenhouseId={}, deviceId={}", greenhouseId, deviceId);
                return;
            }

            Device device = deviceRepository.findById(deviceId).orElse(null);
            String deviceName = device != null ? device.getName() : null;
            processSensorData(greenhouseId, deviceId, deviceName, root);
        }

        /**
         * 传感器数据标准处理流程：写 InfluxDB + 更新状态 + WebSocket 推送 + 告警引擎
         */
        private void processSensorData(Long greenhouseId, Long deviceId, String deviceName, JsonNode root) {
            String sensorType = root.get("sensorType").asText();
            Double value = root.get("value").asDouble();

            // 时间戳可选，为空则使用当前时间
            long timestamp = root.has("timestamp") && !root.get("timestamp").isNull()
                    ? root.get("timestamp").asLong()
                    : System.currentTimeMillis();

            // 写入 InfluxDB
            sensorDataService.writeData(greenhouseId, deviceId, sensorType, value, timestamp);

            // 更新设备状态（标记在线 + 更新最后数据时间和数值）
            String name = sensorDataService.updateDeviceStatus(deviceId, value);

            // WebSocket 实时推送
            pushService.pushSensorData(greenhouseId, deviceId, name, sensorType, value);

            // 预警引擎检测
            alertEngine.check(greenhouseId, deviceId, sensorType, value);

            log.debug("传感器数据已存储并推送: greenhouseId={}, deviceId={}, type={}, value={}",
                    greenhouseId, deviceId, sensorType, value);
        }
    }
}
