package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 问答历史列表项模型
 * <p>
 * 对应后端: GET /api/v1/qa/records
 * </p>
 */
public class QaHistoryItem {

    private Long id;
    private String question;    // 截取前50字
    private String inputType;
    private String asrEngine;
    private String createdAt;

    public QaHistoryItem() {
    }

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public String getInputType() { return inputType; }
    public String getAsrEngine() { return asrEngine; }
    public String getCreatedAt() { return createdAt; }

    /**
     * 是否来自语音输入
     */
    public boolean isVoiceInput() {
        return "VOICE".equals(inputType);
    }
}
