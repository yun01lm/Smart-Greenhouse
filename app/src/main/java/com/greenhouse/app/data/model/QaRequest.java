package com.greenhouse.app.data.model;

/**
 * 文字问答请求模型
 * <p>
 * 对应后端: POST /api/v1/qa/ask
 * </p>
 */
public class QaRequest {

    private String question;
    private Long greenhouseId;

    public QaRequest(String question, Long greenhouseId) {
        this.question = question;
        this.greenhouseId = greenhouseId;
    }

    public String getQuestion() { return question; }
    public Long getGreenhouseId() { return greenhouseId; }
}
