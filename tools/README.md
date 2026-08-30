# 开发工具

## sensor_simulator.py — ESP32 传感器数据模拟器

用于在硬件未到位时模拟 ESP32 设备通过 MQTT 上报传感器数据。

### 用法

```bash
# 安装依赖
pip install paho-mqtt influxdb-client

# 生成历史数据 + 启动实时模拟（默认）
python3 sensor_simulator.py

# 只生成历史数据
python3 sensor_simulator.py --mode history --days 7

# 只启动实时模拟（每5秒发一次）
python3 sensor_simulator.py --mode realtime --interval 5

# 自定义大棚ID
python3 sensor_simulator.py --greenhouse 2
```

### 工作原理

1. **实时模式**：通过 MQTT 协议连接 Mosquitto Broker，模拟 3 组 ESP32 设备定时上报 11 种传感器数据。数据完全走真实链路：MQTT → Spring Boot 后端 → InfluxDB + WebSocket 推送 + 预警检测。

2. **历史模式**：直接写入 InfluxDB，生成带有昼夜温度曲线变化的历史传感器数据，用于 APP 端历史曲线展示测试。

### 硬件到货后

停止脚本即可：`Ctrl+C`。ESP32 上电后会自动替代模拟数据。

---

> **⚠️ 废弃说明（追加）**：`sensor_simulator.py`（旧版模拟器，写死 GREENHOUSE_ID=1、设备ID 101~103，发旧格式 `greenhouse/{ghId}/device/{id}`）已于 R44 固件ID方案落地后**删除**。原因：旧格式 payload 携带的数字 greenhouseId/deviceId 会被后端旧格式兼容分支直接采信，数据会串到大棚1的旧设备上；且存量设备已全部迁移到新格式。现行模拟器为 `simulator/device_simulator.py`（新格式 `device/{firmwareId}/data`，由 `start_all.ps1` 自动启动），上面的用法说明仅作历史存档。
