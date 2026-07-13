package com.greenhouse.module.qa.dto;

import com.greenhouse.entity.QaRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 问答历史列表项 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaHistoryItem {

    /** 记录ID */
    private Long id;

    /** 问题摘要（截取前50字） */
    private String question;

    /** 输入类型 */
    private String inputType;

    /** 语音识别引擎 */
    private String asrEngine;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static QaHistoryItem fromEntity(QaRecord record) {
        String questionPreview = record.getQuestion();
        if (questionPreview != null && questionPreview.length() > 50) {
            questionPreview = questionPreview.substring(0, 50) + "...";
        }

        return QaHistoryItem.builder()
                .id(record.getId())
                .question(questionPreview)
                .inputType(record.getInputType().name())
                .asrEngine(record.getAsrEngine())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
