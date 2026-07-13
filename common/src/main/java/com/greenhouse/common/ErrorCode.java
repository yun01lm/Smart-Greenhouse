package com.greenhouse.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 统一错误码枚举
 * <p>
 * 所有业务错误码集中管理，按模块分段：
 * 1xxx 通用错误 | 2xxx 认证错误 | 3xxx 权限错误 | 4xxx 大棚/设备错误
 * 5xxx AI相关错误 | 6xxx 专家咨询错误 | 7xxx 作物生长周期错误 | 8xxx 文件错误
 * </p>
 */
@Getter
public enum ErrorCode {

    // ===== 通用错误 (1xxx) =====
    SUCCESS(0, "操作成功", HttpStatus.OK),
    PARAM_ERROR(1001, "参数错误", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1002, "资源不存在", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR(1003, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== 认证错误 (2xxx) =====
    UNAUTHORIZED(2001, "未登录或Token已过期", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(2002, "Token无效", HttpStatus.UNAUTHORIZED),
    USERNAME_EXISTS(2003, "用户名已存在", HttpStatus.CONFLICT),
    PHONE_EXISTS(2004, "手机号已注册", HttpStatus.CONFLICT),
    LOGIN_FAILED(2005, "用户名或密码错误", HttpStatus.UNAUTHORIZED),

    // ===== 权限错误 (3xxx) =====
    ACCESS_DENIED(3001, "无访问权限", HttpStatus.FORBIDDEN),
    GREENHOUSE_ACCESS_DENIED(3002, "无该大棚访问权限", HttpStatus.FORBIDDEN),
    FUNCTION_DENIED(3003, "无该功能使用权限", HttpStatus.FORBIDDEN),
    NOT_OWNER(3004, "仅棚主可执行此操作", HttpStatus.FORBIDDEN),
    EMPLOYEE_LIMIT_EXCEEDED(3005, "员工数量已达上限", HttpStatus.FORBIDDEN),

    // ===== 大棚/设备相关 (4xxx) =====
    GREENHOUSE_NOT_FOUND(4001, "大棚不存在", HttpStatus.NOT_FOUND),
    GREENHOUSE_LIMIT_EXCEEDED(4002, "大棚数量已达上限", HttpStatus.BAD_REQUEST),
    DEVICE_NOT_FOUND(4003, "设备不存在", HttpStatus.NOT_FOUND),
    DEVICE_OFFLINE(4004, "设备已离线", HttpStatus.BAD_REQUEST),
    DEVICE_GROUP_NOT_FOUND(4005, "传感器组不存在", HttpStatus.NOT_FOUND),

    // ===== AI相关 (5xxx) =====
    AI_RECOGNITION_FAILED(5001, "图像识别失败，请重试", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_SPEECH_FAILED(5002, "语音识别失败，请尝试文字输入", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_LLM_FAILED(5003, "AI服务暂时不可用", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_EMBEDDING_FAILED(5004, "向量化服务异常", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== 专家咨询 (6xxx) =====
    EXPERT_NOT_FOUND(6001, "专家不存在", HttpStatus.NOT_FOUND),
    EXPERT_OFFLINE(6002, "专家当前不在线", HttpStatus.BAD_REQUEST),
    CONVERSATION_NOT_FOUND(6003, "对话不存在", HttpStatus.NOT_FOUND),
    CONVERSATION_CLOSED(6004, "对话已结束", HttpStatus.BAD_REQUEST),
    AUTHORIZATION_NOT_FOUND(6005, "授权记录不存在", HttpStatus.NOT_FOUND),
    AUTHORIZATION_EXPIRED(6006, "授权已过期", HttpStatus.FORBIDDEN),
    AUTHORIZATION_ALREADY_EXISTS(6007, "已有有效授权，无需重复申请", HttpStatus.CONFLICT),

    // ===== 作物生长周期 (7xxx) =====
    CROP_CYCLE_NOT_FOUND(7001, "生长周期记录不存在", HttpStatus.NOT_FOUND),
    CROP_CYCLE_ALREADY_COMPLETED(7002, "该生长周期已结束", HttpStatus.BAD_REQUEST),
    CROP_CYCLE_DUPLICATE(7003, "该大棚已有进行中的生长周期", HttpStatus.CONFLICT),

    // ===== 文件相关 (8xxx) =====
    FILE_UPLOAD_FAILED(8001, "文件上传失败", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TOO_LARGE(8002, "文件大小超过限制", HttpStatus.BAD_REQUEST),
    FILE_TYPE_NOT_SUPPORTED(8003, "不支持的文件类型", HttpStatus.BAD_REQUEST);

    /** 业务错误码 */
    private final int code;
    /** 错误消息 */
    private final String message;
    /** 对应的HTTP状态码 */
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
