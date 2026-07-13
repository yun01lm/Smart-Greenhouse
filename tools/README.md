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
