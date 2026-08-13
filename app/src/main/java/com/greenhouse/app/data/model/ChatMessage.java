package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 聊天消息模型
 * <p>
 * 对应 GET /api/v1/chat/conversations/{id}/messages 响应。
 * 支持 TEXT / IMAGE / VIDEO / ENV_SNAPSHOT 四种消息类型。
 * </p>
 */
public class ChatMessage {

    private long id;

    @SerializedName("senderId")
    private long senderId;

    @SerializedName("senderType")
    private String senderType; // USER, EXPERT

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("messageType")
    private String messageType; // TEXT, IMAGE, VIDEO, ENV_SNAPSHOT

    private String content;

    @SerializedName("filePath")
    private String filePath;

    private String snapshotData;

    @SerializedName("readStatus")
    private int readStatus; // 0=未读, 1=已读

    @SerializedName("createdAt")
    private String createdAt;

    // ===== Getter =====

    public long getId() { return id; }
    public long getSenderId() { return senderId; }
    public String getSenderType() { return senderType; }
    public String getSenderName() { return senderName; }
    public String getMessageType() { return messageType; }
    public String getContent() { return content; }
    public String getFilePath() { return filePath; }
    public String getSnapshotData() { return snapshotData; }
    public int getReadStatus() { return readStatus; }
    public String getCreatedAt() { return createdAt; }

    // ===== 辅助方法 =====

    public boolean isFromUser() {
        return "USER".equals(senderType);
    }

    public boolean isFromExpert() {
        return "EXPERT".equals(senderType);
    }

    public boolean isText() {
        return "TEXT".equals(messageType);
    }

    public boolean isImage() {
        return "IMAGE".equals(messageType);
    }

    public boolean isVideo() {
        return "VIDEO".equals(messageType);
    }

    public boolean isSnapshot() {
        return "ENV_SNAPSHOT".equals(messageType);
    }

    public boolean isRead() {
        return readStatus == 1;
    }

}
