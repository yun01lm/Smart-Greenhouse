package com.greenhouse.module.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.module.alert.service.AlertEngine;
import com.greenhouse.module.sensor.service.SensorDataService;
import com.greenhouse.module.websocket.service.RealtimePushService;
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
 * 启动后自动订阅设备数据主题（通配符），
 * 接收 ESP32 设备上报的传感器数据并写入 InfluxDB。
 * Topic 常量统一由 {@link MqttTopicConstants} 管理。
 * </p>
 *
 * <h3>ESP32 上报数据格式（JSON）</h3>
 * <pre>{@code
 * {
 *   "greenhouseId": 1,
 *   "deviceId": 5,
 *   "sensorType": "TEMPERATURE",
 *   "value": 25.6,
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

    @PostConstruct
    public void init() {
        try {
            mqttClient.subscribe(MqttTopicConstants.DEVICE_DATA_WILDCARD,
                    MqttTopicConstants.DEFAULT_QOS, new SensorDataListener());
            log.info("MQTT 订阅成功: topic={}", MqttTopicConstants.DEVICE_DATA_WILDCARD);
        } catch (MqttException e) {
            log.error("MQTT 订阅失败: topic={}, error={}",
                    MqttTopicConstants.DEVICE_DATA_WILDCARD, e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (mqttClient.isConnected()) {
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

                Long greenhouseId = root.get("greenhouseId").asLong();
                Long deviceId = root.get("deviceId").asLong();

                // 判断消息类型：CONTROLLER 心跳 vs 传感器数据
                String deviceType = root.has("deviceType") ? root.get("deviceType").asText() : null;
                if ("CONTROLLER".equals(deviceType)) {
                    // 控制器心跳：仅更新设备在线状态
                    sensorDataService.updateDeviceOnline(deviceId);
                    log.debug("控制器心跳已处理: greenhouseId={}, deviceId={}", greenhouseId, deviceId);
                    return;
                }

                // 传感器数据：标准处理流程
                String sensorType = root.get("sensorType").asText();
                Double value = root.get("value").asDouble();

                // 时间戳可选，为空则使用当前时间
                long timestamp = root.has("timestamp") && !root.get("timestamp").isNull()
                        ? root.get("timestamp").asLong()
                        : System.currentTimeMillis();

                // 写入 InfluxDB
                sensorDataService.writeData(greenhouseId, deviceId, sensorType, value, timestamp);

                // 更新设备状态（标记在线 + 更新最后数据时间和数值）
                String deviceName = sensorDataService.updateDeviceStatus(deviceId, value);

                // WebSocket 实时推送
                pushService.pushSensorData(greenhouseId, deviceId, deviceName, sensorType, value);

                // 预警引擎检测
                alertEngine.check(greenhouseId, deviceId, sensorType, value);

                log.debug("传感器数据已存储并推送: greenhouseId={}, deviceId={}, type={}, value={}",
                        greenhouseId, deviceId, sensorType, value);

            } catch (Exception e) {
                log.error("MQTT 消息处理失败: topic={}, error={}", topic, e.getMessage(), e);
            }
        }
    }
}
