#!/usr/bin/env python3
"""
智慧大棚AIoT系统 — ESP32 传感器数据模拟器

功能：
  1. 通过 MQTT 协议模拟多组 ESP32 设备定时上报传感器数据
  2. 预生成历史数据直接写入 InfluxDB（用于 APP 测试展示）
  3. 硬件到货后停止脚本即可切换到真实设备

用法：
  # 只启动实时模拟（每10秒发一条）
  python3 sensor_simulator.py --mode realtime

  # 只生成历史数据（过去7天，每5分钟一条）
  python3 sensor_simulator.py --mode history

  # 同时运行
  python3 sensor_simulator.py --mode both

  # 自定义参数
  python3 sensor_simulator.py --mode both --interval 5 --greenhouse 1 --days 7

MQTT Topic 格式: greenhouse/{greenhouseId}/device/{deviceId}
消息格式 (JSON): {"greenhouseId": 1, "deviceId": 101, "sensorType": "TEMP", "value": 28.5, "timestamp": 1752998400000}
"""

import argparse
import json
import random
import time
import sys
import os
from datetime import datetime, timedelta, timezone
from threading import Thread

import paho.mqtt.client as mqtt
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

# ============================================================
# 配置
# ============================================================

# MQTT（安全加固：启用认证）
MQTT_BROKER = "localhost"
MQTT_PORT = 1883
MQTT_USERNAME = os.environ.get("MQTT_USER", "greenhouse")
MQTT_PASSWORD = os.environ.get("MQTT_PASSWORD", "greenhouse_dev")
MQTT_TOPIC_TEMPLATE = "greenhouse/{greenhouse_id}/device/{device_id}"

# InfluxDB
INFLUX_URL = "http://localhost:8086"
INFLUX_TOKEN = os.environ.get("INFLUX_TOKEN", "dev-token-greenhouse-2026")
INFLUX_ORG = "greenhouse"
INFLUX_BUCKET = "sensor_data"

# 传感器类型（11种参数）
SENSOR_TYPES = [
    "TEMP",        # 空气温度 (°C)
    "HUMIDITY",    # 空气湿度 (%)
    "LIGHT",       # 光照强度 (lux)
    "CO2",         # CO2浓度 (ppm)
    "O2",          # O2浓度 (%)
    "SOIL_TEMP",   # 土壤温度 (°C)
    "SOIL_HUMIDITY",  # 土壤湿度 (%)
    "EC",          # 土壤电导率 (mS/cm)
    "N",           # 氮 (mg/kg)
    "P",           # 磷 (mg/kg)
    "K",           # 钾 (mg/kg)
]

# 各传感器模拟参数：(基准值, 波动范围, 最小值, 最大值)
SENSOR_CONFIG = {
    "TEMP":          (26.0, 3.0, 10.0, 42.0),
    "HUMIDITY":      (65.0, 10.0, 30.0, 95.0),
    "LIGHT":         (25000.0, 8000.0, 0.0, 100000.0),
    "CO2":           (450.0, 80.0, 250.0, 2000.0),
    "O2":            (20.5, 0.5, 17.0, 22.5),
    "SOIL_TEMP":     (22.0, 2.0, 12.0, 32.0),
    "SOIL_HUMIDITY": (55.0, 15.0, 20.0, 85.0),
    "EC":            (1.5, 0.4, 0.3, 3.5),
    "N":             (80.0, 25.0, 10.0, 250.0),
    "P":             (50.0, 15.0, 8.0, 180.0),
    "K":             (120.0, 30.0, 15.0, 350.0),
}

# 模拟大棚配置
GREENHOUSE_ID = 1
DEVICE_GROUPS = [
    {"device_id": 101, "name": "东侧传感器组", "offset": 0},
    {"device_id": 102, "name": "中间传感器组", "offset": 1},
    {"device_id": 103, "name": "西侧传感器组", "offset": 2},
]


def generate_sensor_value(sensor_type, device_offset=0):
    """生成带随机波动的传感器值，不同设备组有微小偏移"""
    config = SENSOR_CONFIG[sensor_type]
    base, variance, vmin, vmax = config

    # 不同设备组有微小偏移（模拟大棚内环境差异）
    offset = device_offset * random.uniform(-0.5, 0.5) * (variance * 0.2)
    base += offset

    # 正态分布随机波动
    value = random.gauss(base, variance * 0.3)
    value = max(vmin, min(vmax, value))

    # 根据传感器类型决定精度
    if sensor_type in ("TEMP", "HUMIDITY", "SOIL_TEMP", "SOIL_HUMIDITY", "O2"):
        value = round(value, 1)
    elif sensor_type in ("LIGHT", "CO2", "N", "P", "K"):
        value = round(value, 0)
    elif sensor_type == "EC":
        value = round(value, 2)

    return value


def build_mqtt_message(greenhouse_id, device_id, sensor_type, value, timestamp_ms=None):
    """构建 MQTT 消息 JSON"""
    if timestamp_ms is None:
        timestamp_ms = int(time.time() * 1000)
    return json.dumps({
        "greenhouseId": greenhouse_id,
        "deviceId": device_id,
        "sensorType": sensor_type,
        "value": value,
        "timestamp": timestamp_ms
    })


def on_connect(client, userdata, flags, reason_code, properties=None):
    if reason_code == 0:
        print("[MQTT] 已连接到 Broker")
    else:
        print(f"[MQTT] 连接失败: {reason_code}")


def on_publish(client, userdata, mid, reason_code, properties=None):
    pass  # 静默


def run_realtime_simulator(interval_sec, greenhouse_id):
    """实时模拟：定时通过 MQTT 发送传感器数据"""
    mqtt_client = mqtt.Client(
        mqtt.CallbackAPIVersion.VERSION2,
        client_id=f"sensor-simulator-{greenhouse_id}"
    )
    if MQTT_USERNAME:
        mqtt_client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)
    mqtt_client.on_connect = on_connect
    mqtt_client.on_publish = on_publish

    try:
        mqtt_client.connect(MQTT_BROKER, MQTT_PORT, keepalive=60)
        mqtt_client.loop_start()
        time.sleep(1)  # 等待连接建立
    except Exception as e:
        print(f"[MQTT] 连接失败: {e}")
        print("[提示] 请确保 Docker 环境已启动: docker-compose up -d")
        return

    print(f"[实时模拟] 启动: greenhouse={greenhouse_id}, 设备组={len(DEVICE_GROUPS)}组, "
          f"传感器={len(SENSOR_TYPES)}种, 间隔={interval_sec}秒")
    print(f"[实时模拟] MQTT Topic: greenhouse/{greenhouse_id}/device/{{device_id}}")
    print("-" * 60)

    msg_count = 0
    try:
        while True:
            for group in DEVICE_GROUPS:
                for sensor_type in SENSOR_TYPES:
                    value = generate_sensor_value(sensor_type, group["offset"])
                    topic = MQTT_TOPIC_TEMPLATE.format(
                        greenhouse_id=greenhouse_id,
                        device_id=group["device_id"]
                    )
                    payload = build_mqtt_message(
                        greenhouse_id, group["device_id"], sensor_type, value
                    )

                    result = mqtt_client.publish(topic, payload, qos=1)
                    msg_count += 1

                    # 每100条打印一次状态
                    if msg_count % 100 == 0:
                        print(f"[实时模拟] 已发送 {msg_count} 条消息, "
                              f"最新: device={group['device_id']}, {sensor_type}={value}")

            time.sleep(interval_sec)

    except KeyboardInterrupt:
        print(f"\n[实时模拟] 停止, 共发送 {msg_count} 条消息")
    finally:
        mqtt_client.loop_stop()
        mqtt_client.disconnect()


def generate_history_data(greenhouse_id, days=7, interval_min=5):
    """预生成历史数据写入 InfluxDB"""
    print(f"[历史数据] 生成中: greenhouse={greenhouse_id}, 天数={days}, 间隔={interval_min}分钟")

    try:
        client = InfluxDBClient(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG)
        write_api = client.write_api(write_options=SYNCHRONOUS)
    except Exception as e:
        print(f"[历史数据] InfluxDB 连接失败: {e}")
        print("[提示] 请确保 Docker 环境已启动: docker-compose up -d")
        return

    now = datetime.now(timezone.utc)
    start_time = now - timedelta(days=days)
    current = start_time

    points = []
    batch_size = 5000
    total = 0
    random.seed(42)  # 固定种子，使历史数据可复现

    # 模拟昼夜温度曲线
    def get_temp_adjustment(hour):
        """根据小时返回温度偏移（模拟昼夜变化）"""
        if 6 <= hour < 12:    # 上午升温
            return (hour - 6) * 0.5
        elif 12 <= hour < 14:  # 中午最高
            return 3.0
        elif 14 <= hour < 18:  # 下午降温
            return 3.0 - (hour - 14) * 0.5
        elif 18 <= hour < 22:  # 傍晚
            return 1.0 - (hour - 18) * 0.3
        else:                  # 夜间
            return -1.0

    print("[历史数据] 正在生成...", end="", flush=True)

    while current <= now:
        hour = current.hour

        for group in DEVICE_GROUPS:
            for sensor_type in SENSOR_TYPES:
                value = generate_sensor_value(sensor_type, group["offset"])

                # 温度类参数叠加昼夜变化
                if sensor_type in ("TEMP", "SOIL_TEMP"):
                    adj = get_temp_adjustment(hour)
                    value += adj * random.uniform(0.8, 1.2)

                point = Point("sensor_data") \
                    .tag("greenhouse_id", str(greenhouse_id)) \
                    .tag("device_id", str(group["device_id"])) \
                    .tag("sensor_type", sensor_type) \
                    .field("value", float(value)) \
                    .time(current, WritePrecision.NS)

                points.append(point)
                total += 1

                if len(points) >= batch_size:
                    write_api.write(bucket=INFLUX_BUCKET, org=INFLUX_ORG, record=points)
                    points = []
                    print(".", end="", flush=True)

        current += timedelta(minutes=interval_min)

    # 写入剩余数据
    if points:
        write_api.write(bucket=INFLUX_BUCKET, org=INFLUX_ORG, record=points)

    client.close()
    print(f"\n[历史数据] 完成! 共写入 {total:,} 条数据, "
          f"时间范围: {start_time.strftime('%Y-%m-%d %H:%M')} ~ {now.strftime('%Y-%m-%d %H:%M')}")


def main():
    parser = argparse.ArgumentParser(
        description="智慧大棚传感器数据模拟器",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  %(prog)s --mode realtime           # 实时模拟
  %(prog)s --mode history            # 只生成历史数据
  %(prog)s --mode both               # 同时运行
  %(prog)s --mode both --interval 5  # 每5秒发送一次
  %(prog)s --mode history --days 30  # 生成30天历史数据
        """
    )
    parser.add_argument("--mode", choices=["realtime", "history", "both"],
                        default="both", help="运行模式 (默认: both)")
    parser.add_argument("--interval", type=int, default=10,
                        help="实时模式发送间隔(秒) (默认: 10)")
    parser.add_argument("--greenhouse", type=int, default=1,
                        help="大棚ID (默认: 1)")
    parser.add_argument("--days", type=int, default=7,
                        help="历史数据天数 (默认: 7)")
    parser.add_argument("--history-interval", type=int, default=5,
                        help="历史数据采样间隔(分钟) (默认: 5)")

    args = parser.parse_args()

    print("=" * 60)
    print("  智慧大棚AIoT系统 — 传感器数据模拟器")
    print("=" * 60)
    print(f"  MQTT Broker: {MQTT_BROKER}:{MQTT_PORT}")
    print(f"  InfluxDB:    {INFLUX_URL}")
    print(f"  大棚ID:      {args.greenhouse}")
    print(f"  设备组:      {[g['device_id'] for g in DEVICE_GROUPS]}")
    print(f"  传感器类型:  {len(SENSOR_TYPES)} 种")
    print("=" * 60)

    if args.mode in ("history", "both"):
        print("\n>>> 第一步：生成历史数据")
        generate_history_data(args.greenhouse, args.days, args.history_interval)

    if args.mode in ("realtime", "both"):
        print("\n>>> 第二步：启动实时模拟")
        print("[提示] 按 Ctrl+C 停止")
        run_realtime_simulator(args.interval, args.greenhouse)


if __name__ == "__main__":
    main()
