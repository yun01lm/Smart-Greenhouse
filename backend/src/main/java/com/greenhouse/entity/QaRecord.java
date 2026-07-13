package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 问答记录实体
 * <p>
 * 对应 DB 第 13 号表 qa_records。
 * 支持文字（TEXT）和语音（VOICE）两种输入方式。
 * </p>
 */
@Entity
@Table(name = "qa_records", indexes = {
        @Index(name = "idx_user_created", columnList = "user_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 问题内容 */
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    /** 回答内容 */
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    /** 输入类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 10)
    private InputType inputType;

    /** 语音识别引擎（VOICE 时填写）：xunfei / whisper */
    @Column(name = "asr_engine", length = 20)
    private String asrEngine;

    /** 引用来源（JSON 格式） */
    @Column(name = "sources", columnDefinition = "JSON")
    private String sources;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** 输入类型枚举 */
    public enum InputType {
        TEXT,   // 文字输入
        VOICE   // 语音输入
    }
}
