package com.greenhouse.module.mqtt;

/**
 * MQTT Topic 常量与工厂方法
 * <p>
 * 统一管理所有 MQTT Topic 的格式和生成逻辑，消除分散在各处的字符串拼接。
 * 后续若需修改 Topic 格式（如增加版本号前缀），只需修改此文件。
 * </p>
 *
 * <h3>Topic 规范</h3>
 * <ul>
 *   <li>设备数据上报：{@code greenhouse/{greenhouseId}/device/{deviceSn}}</li>
 *   <li>设备数据订阅（通配符）：{@code greenhouse/+/device/+}</li>
 *   <li>设备控制下发：{@code greenhouse/{greenhouseId}/device/{deviceSn}/command}</li>
 * </ul>
 *
 * <h3>与 ESP32 固件的协议约定</h3>
 * <p>
 * 此 Topic 格式是后端与 ESP32 固件之间的通信协议。真实硬件和模拟器必须严格遵循。
 * 修改 Topic 格式需同步更新 ESP32 固件代码。
 * </p>
 */
public final class MqttTopicConstants {

    private MqttTopicConstants() {
        // 工具类，禁止实例化
    }

    // ===== Topic 模板 =====

    /** 设备数据上报 Topic 模板 */
    public static final String DEVICE_DATA_TOPIC = "greenhouse/{0}/device/{1}";

    /** 设备数据订阅通配符（订阅所有大棚、所有设备） */
    public static final String DEVICE_DATA_WILDCARD = "greenhouse/+/device/+";

    /** 设备控制下发 Topic 模板 */
    public static final String DEVICE_CONTROL_TOPIC = "greenhouse/{0}/device/{1}/command";

    // ===== MQTT 连接参数 =====

    /** 默认 QoS */
    public static final int DEFAULT_QOS = 1;

    // ===== 工厂方法 =====

    /**
     * 生成设备数据上报 Topic
     *
     * @param greenhouseId 大棚ID
     * @param deviceSn     设备编号
     * @return 如 "greenhouse/1/device/TEMP-001"
     */
    public static String deviceDataTopic(Long greenhouseId, String deviceSn) {
        return "greenhouse/" + greenhouseId + "/device/" + deviceSn;
    }

    /**
     * 生成设备控制下发 Topic
     *
     * @param greenhouseId 大棚ID
     * @param deviceSn     设备编号
     * @return 如 "greenhouse/1/device/PUMP-001/command"
     */
    public static String deviceControlTopic(Long greenhouseId, String deviceSn) {
        return "greenhouse/" + greenhouseId + "/device/" + deviceSn + "/command";
    }
}
