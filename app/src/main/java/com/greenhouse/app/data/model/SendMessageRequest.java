package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 发送消息请求模型
 * <p>
 * 对应 POST /api/v1/chat/messages 请求体。
 * </p>
 */
public class SendMessageRequest {

    @SerializedName("conversationId")
    private long conversationId;

    @SerializedName("messageType")
    private String messageType; // TEXT

    private String content;

    public SendMessageRequest(long conversationId, String content) {
        this.conversationId = conversationId;
        this.messageType = "TEXT";
        this.content = content;
    }

    public long getConversationId() { return conversationId; }
    public String getMessageType() { return messageType; }
    public String getContent() { return content; }
}
