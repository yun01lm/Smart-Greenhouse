package com.greenhouse.common;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 所有业务逻辑异常统一使用此类抛出，由全局异常处理器统一拦截并转换为ApiResponse返回。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " - " + detail);
        this.code = errorCode.getCode();
    }
}
