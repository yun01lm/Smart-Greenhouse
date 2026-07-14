package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 对话信息模型
 * <p>
 * 对应 GET /api/v1/chat/conversations 响应。
 * </p>
 */
public class ConversationInfo {

    private long id;

    @SerializedName("expertId")
    private long expertId;

    @SerializedName("expertName")
    private String expertName;

    @SerializedName("expertSpecialty")
    private String expertSpecialty;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    private String subject;

    private String status; // WAITING, ACTIVE, CLOSED

    @SerializedName("lastMessage")
    private String lastMessage;

    @SerializedName("lastMessageTime")
    private String lastMessageTime;

    @SerializedName("unreadCount")
    private int unreadCount;

    @SerializedName("createdAt")
    private String createdAt;

    // ===== Getter =====

    public long getId() { return id; }
    public long getExpertId() { return expertId; }
    public String getExpertName() { return expertName; }
    public String getExpertSpecialty() { return expertSpecialty; }
    public long getGreenhouseId() { return greenhouseId; }
    public String getSubject() { return subject; }
    public String getStatus() { return status; }
    public String getLastMessage() { return lastMessage; }
    public String getLastMessageTime() { return lastMessageTime; }
    public int getUnreadCount() { return unreadCount; }
    public String getCreatedAt() { return createdAt; }

    // ===== 辅助方法 =====

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean isWaiting() {
        return "WAITING".equals(status);
    }

    public boolean isClosed() {
        return "CLOSED".equals(status);
    }

    public String getStatusText() {
        switch (status) {
            case "WAITING": return "等待中";
            case "ACTIVE": return "进行中";
            case "CLOSED": return "已结束";
            default: return status;
        }
    }

    public boolean hasUnread() {
        return unreadCount > 0;
    }

    public String getUnreadBadge() {
        return unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
    }
}
