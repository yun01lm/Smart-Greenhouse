/*
 * 智慧大棚AIoT系统 — ESP32 固件配置
 * =================================
 * 出厂烧录时只需修改本文件：
 *   1. FIRMWARE_ID —— 出厂预注册的8位数字固件ID（系统 firmwares 表），印在标签上
 *   2. MQTT_HOST   —— 服务器地址（本地开发=局域网IP，生产=公网IP）
 *   3. 传感器类型/引脚
 * WiFi 账号密码不写死：首次上电用配网门户现场配置（见主程序说明）。
 */
#ifndef CONFIG_H
#define CONFIG_H

// ===== 固件身份（出厂预注册，写死；与系统 firmwares 表一致）=====
#define FIRMWARE_ID       "00000001"        // 8位数字，全局唯一，印在标签上
#define DEVICE_TYPE       "SENSOR"          // SENSOR / CONTROLLER
#define SENSOR_TYPE       "TEMPERATURE"     // 传感器类型（SENSOR 固件必填）：
                                            // TEMPERATURE / HUMIDITY / LIGHT / CO2 /
                                            // SOIL_MOISTURE / SOIL_TEMP / SOIL_PH / WIND_SPEED
#define FIRMWARE_VERSION  "1.0.0"           // 固件版本号

// ===== MQTT Broker（出厂写死，与后端 mosquitto 配置一致）=====
#define MQTT_HOST         "192.168.1.100"   // 服务器地址（生产=公网IP）
#define MQTT_PORT         1883
#define MQTT_USER         "greenhouse"
#define MQTT_PASS         "greenhouse_dev"

// ===== 上报周期 =====
#define REPORT_INTERVAL_MS  30000           // 30秒上报一次

// ===== 引脚定义 =====
#define TEMP_SENSOR_PIN   4                 // DHT22 数据引脚（SENSOR 固件）
#define RELAY_PIN         2                 // 继电器控制引脚（CONTROLLER 固件）
#define BOOT_BUTTON_PIN   0                 // BOOT 键（长按5秒清除配网）

// ===== 传感器编译开关 =====
// 使用真实 DHT22 传感器时取消注释；无传感器时走内置模拟值
// #define USE_DHT_SENSOR

#endif
