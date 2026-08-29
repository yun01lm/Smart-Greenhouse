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
 *   <li>固件数据上报（新格式）：{@code device/{firmwareId}/data}</li>
 *   <li>固件数据订阅（通配符）：{@code device/+/data}</li>
 *   <li>固件控制下发（新格式）：{@code device/{firmwareId}/command}</li>
 *   <li>设备数据上报（旧格式，兼容）：{@code greenhouse/{greenhouseId}/device/{deviceSn}}</li>
 *   <li>设备控制下发（旧格式，兼容）：{@code greenhouse/{greenhouseId}/device/{deviceSn}/command}</li>
 * </ul>
 *
 * <h3>与 ESP32 固件的协议约定</h3>
 * <p>
 * 新固件（出厂预注册固件ID）走 {@code device/{firmwareId}/data} 上报、{@code device/{firmwareId}/command}
 * 收指令。旧 Topic 格式保留用于兼容存量设备与模拟器。修改 Topic 格式需同步更新 ESP32 固件代码。
 * </p>
 */
public final class MqttTopicConstants {

    private MqttTopicConstants() {
        // 工具类，禁止实例化
    }

    // ===== Topic 模板 =====

    /** 设备数据上报 Topic 模板（旧格式，按大棚+SN 路由） */
    public static final String DEVICE_DATA_TOPIC = "greenhouse/{0}/device/{1}";

    /** 设备数据订阅通配符（订阅所有大棚、所有设备） */
    public static final String DEVICE_DATA_WILDCARD = "greenhouse/+/device/+";

    /** 设备控制下发 Topic 模板（旧格式） */
    public static final String DEVICE_CONTROL_TOPIC = "greenhouse/{0}/device/{1}/command";

    // ===== 固件 Topic 模板（新格式，按固件ID路由） =====

    /** 固件数据上报 Topic 模板：device/{firmwareId}/data */
    public static final String FIRMWARE_DATA_TOPIC = "device/{0}/data";

    /** 固件数据订阅通配符 */
    public static final String FIRMWARE_DATA_WILDCARD = "device/+/data";

    /** 固件控制下发 Topic 模板：device/{firmwareId}/command */
    public static final String FIRMWARE_CONTROL_TOPIC = "device/{0}/command";

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

    /**
     * 生成固件数据上报 Topic（新格式）
     *
     * @param firmwareId 固件ID（8位数字）
     * @return 如 "device/00000001/data"
     */
    public static String firmwareDataTopic(String firmwareId) {
        return "device/" + firmwareId + "/data";
    }

    /**
     * 生成固件控制下发 Topic（新格式）
     *
     * @param firmwareId 固件ID（8位数字）
     * @return 如 "device/00000001/command"
     */
    public static String firmwareControlTopic(String firmwareId) {
        return "device/" + firmwareId + "/command";
    }
}
