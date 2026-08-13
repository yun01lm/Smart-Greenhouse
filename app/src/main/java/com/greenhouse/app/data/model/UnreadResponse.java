package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 未读消息响应模型
 * <p>
 * 对应 GET /api/v1/chat/unread 响应。
 * </p>
 */
public class UnreadResponse {

    @SerializedName("count")
    private int count;

    @SerializedName("conversations")
    private List<UnreadConversation> conversations;

    public int getTotalUnread() { return count; }
    public List<UnreadConversation> getConversations() { return conversations; }

    public static class UnreadConversation {
        @SerializedName("conversationId")
        private long conversationId;

        @SerializedName("expertName")
        private String expertName;

        @SerializedName("unreadCount")
        private int unreadCount;

        public long getConversationId() { return conversationId; }
        public String getExpertName() { return expertName; }
        public int getUnreadCount() { return unreadCount; }
    }
}
