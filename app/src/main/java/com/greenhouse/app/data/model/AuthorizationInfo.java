package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 授权信息模型
 * <p>
 * 对应 GET /api/v1/expert/authorize/active 和 /pending 响应。
 * </p>
 */
public class AuthorizationInfo {

    private long id;

    @SerializedName("expertId")
    private long expertId;

    @SerializedName("expertName")
    private String expertName;

    @SerializedName("userId")
    private long userId;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("greenhouseName")
    private String greenhouseName;

    private String status; // PENDING, APPROVED, REJECTED, EXPIRED, REVOKED

    private String reason;

    @SerializedName("requestedAt")
    private String requestedAt;

    @SerializedName("approvedAt")
    private String approvedAt;

    @SerializedName("expiresAt")
    private String expiresAt;

    @SerializedName("expiresIn")
    private String expiresIn;

    // ===== Getter =====

    public long getId() { return id; }
    public long getExpertId() { return expertId; }
    public String getExpertName() { return expertName; }
    public long getUserId() { return userId; }
    public long getGreenhouseId() { return greenhouseId; }
    public String getGreenhouseName() { return greenhouseName; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public String getRequestedAt() { return requestedAt; }
    public String getApprovedAt() { return approvedAt; }
    public String getExpiresAt() { return expiresAt; }
    public String getExpiresIn() { return expiresIn; }

    // ===== 辅助方法 =====

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isExpired() {
        return "EXPIRED".equals(status);
    }

    public String getStatusText() {
        switch (status) {
            case "PENDING": return "待处理";
            case "APPROVED": return "已授权";
            case "REJECTED": return "已拒绝";
            case "EXPIRED": return "已过期";
            case "REVOKED": return "已撤销";
            default: return status;
        }
    }
}
