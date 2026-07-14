package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 环境快照请求模型
 * <p>
 * 对应 POST /api/v1/chat/snapshot 请求体。
 * </p>
 */
public class SnapshotRequest {

    @SerializedName("conversationId")
    private long conversationId;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    public SnapshotRequest(long conversationId, long greenhouseId) {
        this.conversationId = conversationId;
        this.greenhouseId = greenhouseId;
    }

    public long getConversationId() { return conversationId; }
    public long getGreenhouseId() { return greenhouseId; }
}
