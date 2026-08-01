package com.greenhouse.common;

/**
 * 业务错误码枚举
 */
public enum ErrorCode {

    // 通用
    SUCCESS(200, "操作成功"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    PARAM_ERROR(400, "参数错误"),
    RESOURCE_NOT_FOUND(404, "资源不存在"),
    ACCESS_DENIED(403, "无权访问"),

    // 认证相关
    LOGIN_FAILED(1001, "用户名或密码错误"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    PHONE_EXISTS(1003, "手机号已存在"),
    NOT_OWNER(1004, "不是棚主角色"),

    // 大棚相关
    GREENHOUSE_NOT_FOUND(2001, "大棚不存在"),
    GREENHOUSE_ACCESS_DENIED(2002, "无权访问该大棚"),
    GREENHOUSE_LIMIT_EXCEEDED(2003, "大棚数量已达上限"),

    // 员工相关
    EMPLOYEE_LIMIT_EXCEEDED(3001, "员工数量已达上限"),

    // AI相关
    AI_SPEECH_FAILED(4001, "语音识别失败"),
    AI_QA_FAILED(4002, "AI问答失败"),

    // MQTT相关
    MQTT_SEND_FAILED(5001, "MQTT消息发送失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
