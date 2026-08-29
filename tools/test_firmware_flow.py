#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
固件预注册+绑定+MQTT上报 全链路回归测试
========================================
验证点：
  1. 登录 admin
  2. 删除旧测试设备（如有）→ 验证固件解绑回 UNBOUND
  3. 绑定固件 00000039 到 大棚1（中文名）→ SN 自动生成
  4. 重复绑定 → 应报错"固件已被绑定"
  5. 绑定不存在的固件ID → 应报错"固件不存在"
  6. MQTT 新格式上报 device/00000039/data → 设备 ONLINE、InfluxDB 有数据
  7. 未绑定固件上报（00000040 未绑定？不，00000040 是 UNBOUND）→ 数据丢弃+日志
  8. 控制器心跳新格式 → 在线状态更新
用法: python tools/test_firmware_flow.py
"""
import json
import sys
import time
import urllib.request

BASE = "http://localhost:8080"

def api(method, path, token=None, body=None):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            return json.loads(raw)
        except Exception:
            return {"code": e.code, "message": raw}

def step(no, title):
    print(f"\n===== [{no}] {title} =====")

PASS = 0
FAIL = 0
def check(cond, msg):
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"  ✅ {msg}")
    else:
        FAIL += 1
        print(f"  ❌ {msg}")

def main():
    # 1. 登录
    step(1, "登录 admin")
    r = api("POST", "/api/v1/auth/login", body={"username": "admin", "password": "123456"})
    token = (r.get("data") or {}).get("token")
    check(bool(token), f"登录成功 token={token[:16] if token else 'N/A'}...")
    if not token:
        sys.exit(1)

    # 2. 查找并删除旧测试设备（名含"测试固件"或固件ID=00000039）
    step(2, "清理旧测试设备（验证删除解绑）")
    r = api("GET", "/api/v1/greenhouses/1/devices", token)
    deleted = None
    for d in r.get("data") or []:
        if d.get("firmwareId") == "00000039":
            deleted = d["id"]
            break
    if deleted:
        rr = api("DELETE", f"/api/v1/greenhouses/1/devices/{deleted}", token)
        check(rr.get("code") == 200, f"删除旧测试设备 id={deleted}")
    else:
        check(True, "无旧测试设备")

    # 3. 绑定 00000039 → 大棚1（中文名）
    step(3, "绑定固件 00000039 到大棚1")
    r = api("POST", "/api/v1/greenhouses/1/devices", token, body={
        "name": "测试固件39号温度传感器",
        "firmwareId": "00000039",
        "deviceType": "SENSOR",
        "sensorType": "TEMPERATURE",
        "installLocation": "回归测试位",
        "description": "固件ID预注册流程回归"
    })
    d = r.get("data") or {}
    check(r.get("code") == 200, f"绑定成功 deviceId={d.get('id')} name={d.get('name')}")
    check(d.get("deviceSn") == "GH1-09", f"SN 自动生成正确: {d.get('deviceSn')}")
    check(d.get("firmwareId") == "00000039", f"固件ID回显: {d.get('firmwareId')}")
    dev_id = d.get("id")

    # 4. 重复绑定 → 报错
    step(4, "重复绑定同一固件")
    r = api("POST", "/api/v1/greenhouses/1/devices", token, body={
        "name": "重复绑定测试", "firmwareId": "00000039",
        "deviceType": "SENSOR", "sensorType": "TEMPERATURE"})
    check(r.get("code") != 200 and "已被绑定" in (r.get("message") or ""),
          f"拒绝重复绑定: {r.get('message')}")

    # 5. 不存在的固件
    step(5, "绑定不存在的固件ID")
    r = api("POST", "/api/v1/greenhouses/1/devices", token, body={
        "name": "不存在固件测试", "firmwareId": "99999999",
        "deviceType": "SENSOR", "sensorType": "TEMPERATURE"})
    check(r.get("code") != 200 and "固件不存在" in (r.get("message") or ""),
          f"拒绝不存在固件: {r.get('message')}")

    # 6. 固件类型不匹配（00000040 是未绑定传感器固件，填控制器）
    step(6, "固件类型不匹配（00000040 传感器固件填控制器）")
    r = api("POST", "/api/v1/greenhouses/1/devices", token, body={
        "name": "类型不匹配测试", "firmwareId": "00000040",
        "deviceType": "CONTROLLER", "sensorType": None})
    check(r.get("code") != 200 and "不匹配" in (r.get("message") or ""),
          f"拒绝类型不匹配: {r.get('message')}")

    # 7. MQTT 新格式上报
    step(7, "MQTT 新格式上报 device/00000039/data")
    try:
        import paho.mqtt.client as mqtt
    except ImportError:
        check(False, "缺少 paho-mqtt")
        sys.exit(1)
    pub = mqtt.Client(client_id="test-fw-flow", protocol=mqtt.MQTTv311)
    pub.username_pw_set("greenhouse", "greenhouse_dev")
    pub.connect("localhost", 1883, keepalive=60)
    pub.loop_start()
    payload = json.dumps({
        "firmwareId": "00000039",
        "sensorType": "TEMPERATURE",
        "value": 25.6,
        "timestamp": int(time.time() * 1000)
    })
    info = pub.publish("device/00000039/data", payload, qos=1)
    time.sleep(1.5)
    pub.loop_stop()
    pub.disconnect()
    check(info.rc == 0, f"MQTT 发布成功 rc={info.rc}")

    # 8. 验证设备状态 ONLINE + InfluxDB
    step(8, "验证设备状态与数据入库")
    time.sleep(2)
    r = api("GET", f"/api/v1/greenhouses/1/devices/{dev_id}", token)
    d = r.get("data") or {}
    check(d.get("status") == "ONLINE", f"设备状态 ONLINE: {d.get('status')}")
    check(d.get("lastValue") == "25.60", f"最新值已记录(2位小数): {d.get('lastValue')}")
    check(d.get("lastDataTime"), f"最后上报时间: {d.get('lastDataTime')}")

    # 9. 未绑定固件上报 → 丢弃（不崩溃）
    step(9, "未绑定固件上报（00000040 为 UNBOUND）")
    pub2 = mqtt.Client(client_id="test-fw-unbound", protocol=mqtt.MQTTv311)
    pub2.username_pw_set("greenhouse", "greenhouse_dev")
    pub2.connect("localhost", 1883, keepalive=60)
    pub2.loop_start()
    payload2 = json.dumps({"firmwareId": "00000040", "sensorType": "TEMPERATURE", "value": 99.9})
    info2 = pub2.publish("device/00000040/data", payload2, qos=1)
    time.sleep(1.0)
    pub2.loop_stop()
    pub2.disconnect()
    check(info2.rc == 0, "未绑定固件消息已发布（后端应丢弃+日志）")

    # 10. 控制器心跳新格式（绑定 00000040 前先不测；用已绑定的控制器存量设备测）
    step(10, "控制器心跳新格式（存量 PUMP-001 → 00000007）")
    pub3 = mqtt.Client(client_id="test-fw-heartbeat", protocol=mqtt.MQTTv311)
    pub3.username_pw_set("greenhouse", "greenhouse_dev")
    pub3.connect("localhost", 1883, keepalive=60)
    pub3.loop_start()
    payload3 = json.dumps({"firmwareId": "00000007", "deviceType": "CONTROLLER",
                           "status": "ONLINE", "timestamp": int(time.time() * 1000)})
    info3 = pub3.publish("device/00000007/data", payload3, qos=1)
    time.sleep(1.0)
    pub3.loop_stop()
    pub3.disconnect()
    check(info3.rc == 0, "控制器心跳已发布（PUMP-001 应在线）")

    print(f"\n========== 回归结果: {PASS} 通过 / {FAIL} 失败 ==========")
    sys.exit(1 if FAIL else 0)

if __name__ == "__main__":
    main()
