package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * AI 问答响应模型
 * <p>
 * 对应后端: POST /api/v1/qa/ask 和 POST /api/v1/qa/ask/voice
 * </p>
 */
public class QaResponse {

    private Long id;
    private String question;

    @SerializedName("rawDialectText")
    private String rawDialectText;

    private String answer;
    private String inputType;   // TEXT / VOICE
    private String asrEngine;
    private String dialect;
    private List<SourceInfo> sources;
    private String createdAt;

    public QaResponse() {
    }

    // ===== Getter =====

    public Long getId() { return id; }
    public String getQuestion() { return question; }
    public String getRawDialectText() { return rawDialectText; }
    public String getAnswer() { return answer; }
    public String getInputType() { return inputType; }
    public String getAsrEngine() { return asrEngine; }
    public String getDialect() { return dialect; }
    public List<SourceInfo> getSources() { return sources; }
    public String getCreatedAt() { return createdAt; }


    // ===== Setter =====



    public void setQuestion(String question) { this.question = question; }

    public void setAnswer(String answer) { this.answer = answer; }

    public void setInputType(String inputType) { this.inputType = inputType; }

    public void setSources(List<SourceInfo> sources) { this.sources = sources; }

    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }



    /**

     * 从历史记录构建（用于恢复历史对话气泡）

     */

    public static QaResponse fromHistory(QaHistoryItem item) {

        QaResponse resp = new QaResponse();

        resp.setQuestion(item.getQuestion());

        resp.setAnswer(item.getAnswer());

        resp.setInputType(item.getInputType());

        resp.setSources(item.getSources());

        resp.setCreatedAt(item.getCreatedAt());

        return resp;

    }

    /**
     * 是否来自语音输入
     */
    public boolean isVoiceInput() {
        return "VOICE".equals(inputType);
    }

    /**
     * 引用来源内部类
     */
    public static class SourceInfo {
        private String title;
        private String category;

        public SourceInfo() {
        }

        public String getTitle() { return title; }
        public String getCategory() { return category; }
    }
}
