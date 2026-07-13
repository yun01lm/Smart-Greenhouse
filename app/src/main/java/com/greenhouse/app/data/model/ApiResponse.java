package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 后端统一响应格式
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... }
 * }
 * </pre>
 */
public class ApiResponse<T> {

    private int code;
    private String message;

    @SerializedName("data")
    private T data;

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }

    public boolean isSuccess() {
        return code == 200;
    }
}
