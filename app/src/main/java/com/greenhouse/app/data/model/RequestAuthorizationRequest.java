package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 请求授权 — 请求体 (F10)
 * <p>
 * 对应后端 POST /api/v1/expert/authorize/request
 * </p>
 */
public class RequestAuthorizationRequest {

    @SerializedName("userId")
    private long userId;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("reason")
    private String reason;

    public RequestAuthorizationRequest(long userId, long greenhouseId, String reason) {
        this.userId = userId;
        this.greenhouseId = greenhouseId;
        this.reason = reason;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getGreenhouseId() {
        return greenhouseId;
    }

    public void setGreenhouseId(long greenhouseId) {
        this.greenhouseId = greenhouseId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
