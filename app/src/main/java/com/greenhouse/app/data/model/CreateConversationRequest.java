package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 创建对话请求模型
 * <p>
 * 对应 POST /api/v1/chat/conversations 请求体。
 * </p>
 */
public class CreateConversationRequest {

    @SerializedName("expertId")
    private long expertId;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    private String subject;

    @SerializedName("diagnosticId")
    private Long diagnosticId;

    public CreateConversationRequest(long expertId, long greenhouseId, String subject) {
        this.expertId = expertId;
        this.greenhouseId = greenhouseId;
        this.subject = subject;
    }

    public CreateConversationRequest(long expertId, long greenhouseId, String subject, Long diagnosticId) {
        this.expertId = expertId;
        this.greenhouseId = greenhouseId;
        this.subject = subject;
        this.diagnosticId = diagnosticId;
    }

    public long getExpertId() { return expertId; }
    public long getGreenhouseId() { return greenhouseId; }
    public String getSubject() { return subject; }
    public Long getDiagnosticId() { return diagnosticId; }
}
