# 智慧大棚 ESP32 固件模板

出厂预注册固件ID + WiFiManager 现场配网 + MQTT 上报/收指令。

## 目录

- `greenhouse_esp32.ino` — 主程序
- `config.h` — 出厂配置（固件ID / broker / 引脚）

## 烧录流程

1. Arduino IDE 安装 ESP32 支持（开发板管理器搜 `esp32`），选开发板 `ESP32 Dev Module`；
2. 库管理器安装：`WiFiManager`、`PubSubClient`、`ArduinoJson`（传感器固件另加 `DHT sensor library`）；
3. 修改 `config.h`：
   - `FIRMWARE_ID` 填系统里预注册的 8 位固件ID；
   - `DEVICE_TYPE` / `SENSOR_TYPE` 与固件档案一致；
   - `MQTT_HOST` 填服务器地址；
   - 接真实 DHT22 时取消 `USE_DHT_SENSOR` 注释；
4. 编译烧录。

## 现场使用

| 步骤 | 操作 |
|---|---|
| 配网 | 首次上电自动开热点 `GH-{固件ID}`（密码 12345678），手机连接后访问 `192.168.4.1`，填自家 WiFi 账号密码 |
| 换网/换地 | 长按 BOOT 键 5 秒，清除配网信息重新配网 |
| 用户添加设备 | 在 Web/APP 填标签上的固件ID + 设备名称 + 类型，系统自动生成设备编号 |

## MQTT 协议

```
上报:  device/{firmwareId}/data
       {"firmwareId":"00000001","sensorType":"TEMPERATURE","value":25.6,"timestamp":1753088400000}
心跳:  device/{firmwareId}/data
       {"firmwareId":"00000002","deviceType":"CONTROLLER","timestamp":1753088400000}
命令:  订阅 device/{firmwareId}/command
       {"action":"ON","timestamp":1753088400000}
```

## 标签模板

打印标签见 `tools/device_label.html`（固件ID + 型号 + 二维码，二维码内容为固件ID，可扫码快速添加）。
