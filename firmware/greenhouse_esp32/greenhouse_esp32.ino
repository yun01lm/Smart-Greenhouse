/*
 * 智慧大棚AIoT系统 — ESP32 固件模板
 * ===================================
 * 协议（与后端 MqttTopicConstants.java / MqttSubscriber.java 对齐）：
 *   上报:  device/{firmwareId}/data
 *         {"firmwareId":"00000001","sensorType":"TEMPERATURE","value":25.6,"timestamp":1753088400000}
 *   心跳:  device/{firmwareId}/data
 *         {"firmwareId":"00000002","deviceType":"CONTROLLER","timestamp":1753088400000}
 *   命令:  订阅 device/{firmwareId}/command
 *         {"action":"ON","timestamp":1753088400000}
 *
 * 配网：首次上电自动开启配网门户（WiFiManager），手机连热点 "GH-{固件ID}"，
 *       浏览器访问 192.168.4.1 只填 WiFi 账号密码（大棚ID无需填写，由后端按固件ID定位）。
 *       长按 BOOT 键 5 秒清除配网信息，重新进入配网模式。
 *
 * 依赖库（Arduino IDE 库管理器安装）：
 *   - WiFiManager（tzapu/WiFiManager）
 *   - PubSubClient（knolleary/PubSubClient）
 *   - ArduinoJson（bblanchon/ArduinoJson）
 *   - DHT sensor library（Adafruit，仅传感器固件需要）
 *
 * 开发板：ESP32 Dev Module（Arduino-ESP32 core 2.x）
 */
#include <WiFi.h>
#include <WiFiManager.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include "config.h"

#ifdef USE_DHT_SENSOR
#include <DHT.h>
#endif

// ===== 全局对象 =====
WiFiClient espClient;
PubSubClient mqttClient(espClient);

// 数据topic与命令topic
String dataTopic;
String commandTopic;

unsigned long lastReport = 0;
unsigned long lastReconnectAttempt = 0;
bool configModeRequested = false;
unsigned long bootButtonPressedAt = 0;

// 继电器状态（控制器固件）
bool relayState = false;

// ===== 配网 =====
void setupWiFi() {
  WiFi.mode(WIFI_STA);

  WiFiManager wm;
  // 配网门户名称带固件ID后缀，方便用户识别
  String apName = "GH-" + String(FIRMWARE_ID);
  wm.setConfigPortalTimeout(180); // 3分钟无操作自动关闭

  // 长按BOOT进入配网时强制打开门户
  bool forcePortal = configModeRequested;
  if (!wm.autoConnect(apName.c_str(), "12345678")) {
    Serial.println("[WiFi] 配网失败，重启重试");
    ESP.restart();
  }
  Serial.print("[WiFi] 已连接: ");
  Serial.println(WiFi.localIP());
}

// 清除配网信息（长按BOOT触发）
void eraseWiFiConfig() {
  WiFiManager wm;
  wm.resetSettings();
  Serial.println("[WiFi] 配网信息已清除，重启进入配网模式");
  ESP.restart();
}

// ===== MQTT =====
void connectMqtt() {
  while (!mqttClient.connected()) {
    Serial.print("[MQTT] 连接中...");
    String clientId = String("gh-") + FIRMWARE_ID;
    if (mqttClient.connect(clientId.c_str(), MQTT_USER, MQTT_PASS)) {
      Serial.println(" 已连接");
      mqttClient.subscribe(commandTopic.c_str(), 1);
      Serial.print("[MQTT] 已订阅命令: ");
      Serial.println(commandTopic);
    } else {
      Serial.print(" 失败 rc=");
      Serial.println(mqttClient.state());
      delay(3000);
    }
  }
}

void publishSensorData(float value) {
  StaticJsonDocument<256> doc;
  doc["firmwareId"] = FIRMWARE_ID;
  doc["sensorType"] = SENSOR_TYPE;
  doc["value"] = value;
  doc["timestamp"] = millis() * 1UL + 1700000000000UL; // 简易时间戳，建议改用NTP
  // 注意：真实部署建议启用 NTP 获取 Unix 毫秒时间戳，此处仅为占位

  char buffer[256];
  size_t n = serializeJson(doc, buffer);
  bool ok = mqttClient.publish(dataTopic.c_str(), buffer, 1);
  Serial.print("[上报] ");
  Serial.print(dataTopic);
  Serial.print(" → ");
  Serial.println(buffer);
  (void)ok;
}

void publishControllerHeartbeat() {
  StaticJsonDocument<256> doc;
  doc["firmwareId"] = FIRMWARE_ID;
  doc["deviceType"] = "CONTROLLER";
  doc["timestamp"] = millis() * 1UL + 1700000000000UL;

  char buffer[256];
  size_t n = serializeJson(doc, buffer);
  mqttClient.publish(dataTopic.c_str(), buffer, 1);
  Serial.print("[心跳] ");
  Serial.println(buffer);
  (void)n;
}

// 命令回调：{"action":"ON"/"OFF","timestamp":...}
void onCommand(char* topic, byte* payload, unsigned int length) {
  StaticJsonDocument<256> doc;
  DeserializationError err = deserializeJson(doc, payload, length);
  if (err) {
    Serial.println("[命令] JSON 解析失败");
    return;
  }

  const char* action = doc["action"] | "";
  if (strcmp(action, "ON") == 0) {
    relayState = true;
    digitalWrite(RELAY_PIN, HIGH);
    Serial.println("[命令] 继电器 ON");
  } else if (strcmp(action, "OFF") == 0) {
    relayState = false;
    digitalWrite(RELAY_PIN, LOW);
    Serial.println("[命令] 继电器 OFF");
  } else {
    Serial.print("[命令] 未知动作: ");
    Serial.println(action);
  }
}

// ===== 传感器读取 =====
#ifdef USE_DHT_SENSOR
DHT dht(TEMP_SENSOR_PIN, DHT22);
float readSensorValue() {
  float t = dht.readTemperature();
  if (isnan(t)) {
    Serial.println("[传感器] DHT22 读取失败");
    return -127.0f;
  }
  return t;
}
#else
// 无真实传感器时返回模拟值（接真实传感器后替换此函数）
float readSensorValue() {
  return 20.0f + 5.0f * sin(millis() / 60000.0f);
}
#endif

// ===== 主流程 =====
void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.println("\n===== 智慧大棚 ESP32 固件启动 =====");
  Serial.print("固件ID: ");
  Serial.println(FIRMWARE_ID);

  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);
  pinMode(BOOT_BUTTON_PIN, INPUT_PULLUP);

#ifdef USE_DHT_SENSOR
  dht.begin();
#endif

  setupWiFi();

  dataTopic = String("device/") + FIRMWARE_ID + "/data";
  commandTopic = String("device/") + FIRMWARE_ID + "/command";

  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(onCommand);
  connectMqtt();

  // 上电立即上报一帧，让后端尽快识别
  if (strcmp(DEVICE_TYPE, "CONTROLLER") == 0) {
    publishControllerHeartbeat();
  } else {
    publishSensorData(readSensorValue());
  }
}

void loop() {
  // 长按 BOOT 键 5 秒清除配网
  if (digitalRead(BOOT_BUTTON_PIN) == LOW) {
    if (bootButtonPressedAt == 0) bootButtonPressedAt = millis();
    if (millis() - bootButtonPressedAt > 5000) {
      Serial.println("[配网] 长按触发，清除配网信息");
      eraseWiFiConfig();
    }
  } else {
    bootButtonPressedAt = 0;
  }

  // MQTT 断线重连
  if (!mqttClient.connected()) {
    unsigned long now = millis();
    if (now - lastReconnectAttempt > 5000) {
      lastReconnectAttempt = now;
      connectMqtt();
    }
  } else {
    mqttClient.loop();
  }

  // 周期上报
  if (millis() - lastReport >= REPORT_INTERVAL_MS) {
    lastReport = millis();
    if (strcmp(DEVICE_TYPE, "CONTROLLER") == 0) {
      publishControllerHeartbeat();
    } else {
      publishSensorData(readSensorValue());
    }
  }
}
