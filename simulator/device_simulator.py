#!/usr/bin/env python3
"""
智慧大棚AIoT系统 — MQTT 设备模拟器
====================================

功能：模拟 ESP32 设备通过 MQTT 协议上报传感器数据。
当真实 ESP32 硬件不可用时，使用此模拟器为系统提供测试数据。

协议约定（必须严格遵守）：
  - MQTT Topic: greenhouse/{greenhouseId}/device/{deviceSn}
  - 数据格式 (JSON): {"greenhouseId": 1, "deviceId": 5, "sensorType": "TEMPERATURE", "value": 25.6, "timestamp": 1753088400000}
  - 此协议与后端 MqttTopicConstants.java 和 MqttSubscriber.java 完全对齐

支持三种运行模式：
  1. normal      — 正常环境模式（日常演示）
  2. abnormal    — 异常环境模式（告警测试）
  3. disease_risk — 病害风险模式（AI 诊断/多模态融合测试）

用法：
  python3 device_simulator.py [--mode normal|abnormal|disease_risk] [--config devices.json]

依赖：
  pip3 install paho-mqtt
"""

import argparse
import json
import random
import sys
import time
import signal
from datetime import datetime


class DeviceSimulator:
    """MQTT 设备模拟器 — 严格遵循真实 ESP32 协议"""

    def __init__(self, config_path: str, mode: str):
        self.config = self._load_config(config_path)
        self.mode = mode
        self.running = True
        self.client = None
        self._validate_mode()

    def _load_config(self, path: str) -> dict:
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)

    def _validate_mode(self):
        valid_modes = list(self.config['modes'].keys())
        if self.mode not in valid_modes:
            print(f"[错误] 未知模式 '{self.mode}'，可选: {valid_modes}")
            sys.exit(1)

    def start(self):
        """启动模拟器：连接 MQTT Broker 并开始循环上报"""
        self._connect_mqtt()
        self._print_banner()

        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)

        try:
            self._run_loop()
        except KeyboardInterrupt:
            pass
        finally:
            self._cleanup()

    def _connect_mqtt(self):
        """连接 MQTT Broker"""
        try:
            import paho.mqtt.client as mqtt
        except ImportError:
            print("[错误] 缺少 paho-mqtt 库，请执行: pip3 install paho-mqtt")
            sys.exit(1)

        mqtt_cfg = self.config['mqtt']
        self.client = mqtt.Client(
            client_id=mqtt_cfg.get('client_id', 'simulator-greenhouse-01'),
            protocol=mqtt.MQTTv311
        )

        if mqtt_cfg.get('username'):
            self.client.username_pw_set(mqtt_cfg['username'], mqtt_cfg.get('password', ''))

        try:
            self.client.connect(mqtt_cfg['broker'], mqtt_cfg['port'], keepalive=60)
            self.client.loop_start()
            print(f"[MQTT] 已连接到 {mqtt_cfg['broker']}:{mqtt_cfg['port']}")
        except Exception as e:
            print(f"[错误] MQTT 连接失败: {e}")
            print("[提示] 请确保 Mosquitto Broker 已启动: mosquitto -d")
            sys.exit(1)

    def _print_banner(self):
        mode_desc = self.config['modes'][self.mode]['description']
        greenhouse_count = len(self.config['greenhouses'])
        device_count = sum(len(gh['devices']) for gh in self.config['greenhouses'])

        print("=" * 60)
        print("  智慧大棚AIoT系统 — MQTT 设备模拟器")
        print("=" * 60)
        print(f"  运行模式: {self.mode} — {mode_desc}")
        print(f"  模拟大棚: {greenhouse_count} 个")
        print(f"  模拟设备: {device_count} 个")
        print(f"  MQTT Topic: greenhouse/{{greenhouseId}}/device/{{deviceSn}}")
        print("=" * 60)
        print("  按 Ctrl+C 停止模拟器")
        print()

    def _run_loop(self):
        """主循环：按设备配置的上报间隔发送 MQTT 消息"""
        # 记录每个设备上次上报时间
        last_report = {}

        while self.running:
            now = time.time()

            for greenhouse in self.config['greenhouses']:
                for device in greenhouse['devices']:
                    device_key = f"{greenhouse['id']}_{device['id']}"
                    interval = device.get('interval_seconds', 10)

                    if device_key not in last_report or (now - last_report[device_key]) >= interval:
                        self._publish_sensor_data(greenhouse, device)
                        last_report[device_key] = now

            time.sleep(1)

    def _publish_sensor_data(self, greenhouse: dict, device: dict):
        """生成传感器数据并通过 MQTT 发布"""
        # 根据运行模式选择参数范围
        mode_params = device[self.mode]
        value = self._generate_value(mode_params['min'], mode_params['max'], mode_params['noise'])

        # 构建与真实 ESP32 完全一致的 JSON 数据
        payload = {
            "greenhouseId": greenhouse['id'],
            "deviceId": device['id'],
            "sensorType": device['sensor_type'],
            "value": round(value, 2),
            "timestamp": int(time.time() * 1000)  # epoch 毫秒
        }

        # 构建 MQTT Topic（严格遵循协议）
        topic = f"greenhouse/{greenhouse['id']}/device/{device['sn']}"

        try:
            json_payload = json.dumps(payload, ensure_ascii=False)
            result = self.client.publish(topic, json_payload, qos=1)

            if result.rc == 0:
                ts = datetime.now().strftime('%H:%M:%S')
                print(f"  [{ts}] {topic} → {device['sensor_type']}={value:.1f}")
            else:
                print(f"  [警告] 发布失败: {topic}, rc={result.rc}")

        except Exception as e:
            print(f"  [错误] 发布异常: {topic}, {e}")

    def _generate_value(self, min_val: float, max_val: float, noise: float) -> float:
        """生成在 [min, max] 范围内带随机噪声的值"""
        base = random.uniform(min_val, max_val)
        noise_val = random.gauss(0, noise)
        return base + noise_val

    def _signal_handler(self, signum, frame):
        print("\n[信息] 收到停止信号，正在关闭模拟器...")
        self.running = False

    def _cleanup(self):
        """清理资源"""
        if self.client:
            self.client.loop_stop()
            self.client.disconnect()
            print("[MQTT] 已断开连接")
        print("[信息] 设备模拟器已停止")


def main():
    parser = argparse.ArgumentParser(
        description='智慧大棚AIoT系统 — MQTT 设备模拟器',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
模式说明:
  normal      — 正常环境，传感器值在适宜范围内（日常演示）
  abnormal    — 异常环境，传感器值偏离适宜范围（告警测试）
  disease_risk — 病害风险，高温高湿低光照（AI 诊断/多模态融合测试）

示例:
  python3 device_simulator.py
  python3 device_simulator.py --mode abnormal
  python3 device_simulator.py --mode disease_risk --config devices.json
        """
    )
    parser.add_argument(
        '--mode', '-m',
        choices=['normal', 'abnormal', 'disease_risk'],
        default='normal',
        help='运行模式（默认: normal）'
    )
    parser.add_argument(
        '--config', '-c',
        default='devices.json',
        help='设备配置文件路径（默认: devices.json）'
    )

    args = parser.parse_args()

    simulator = DeviceSimulator(args.config, args.mode)
    simulator.start()


if __name__ == '__main__':
    main()
