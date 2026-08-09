package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 问答历史列表项模型
 * <p>
 * 对应后端: GET /api/v1/qa/records
 * </p>
 */
public class QaHistoryItem {

    private Long id;
    private String question;    // 问题内容（完整）
    private String answer;      // 回答内容（完整，供恢复对话）
    private String inputType;
    private String asrEngine;
    private List<QaResponse.SourceInfo> sources;  // 引用来源
    private String createdAt;

    public QaHistoryItem() {
    }

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getInputType() { return inputType; }
    public String getAsrEngine() { return asrEngine; }
    public List<QaResponse.SourceInfo> getSources() { return sources; }
    public String getCreatedAt() { return createdAt; }

    /**
     * 是否来自语音输入
     */
    public boolean isVoiceInput() {
        return "VOICE".equals(inputType);
    }
}