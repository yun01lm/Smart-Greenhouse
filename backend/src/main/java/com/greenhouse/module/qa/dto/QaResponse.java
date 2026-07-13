package com.greenhouse.module.qa.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.entity.QaRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI 问答响应 DTO
 * <p>
 * 包含问题、回答、引用来源、语音识别信息等完整字段。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QaResponse {

    /** 记录ID */
    private Long id;

    /** 问题内容 */
    private String question;

    /** 回答内容 */
    private String answer;

    /** 输入类型 */
    private String inputType;

    /** 语音识别引擎（VOICE时） */
    private String asrEngine;

    /** 方言类型 */
    private String dialect;

    /** 引用来源 */
    private List<SourceInfo> sources;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 引用来源信息 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceInfo {
        private String title;
        private String category;
    }

    /**
     * 从实体转换（用于文字问答）
     */
    public static QaResponse fromEntity(QaRecord record) {
        return QaResponse.builder()
                .id(record.getId())
                .question(record.getQuestion())
                .answer(record.getAnswer())
                .inputType(record.getInputType().name())
                .asrEngine(record.getAsrEngine())
                .dialect(null)
                .sources(parseSources(record.getSources()))
                .createdAt(record.getCreatedAt())
                .build();
    }

    /**
     * 从实体转换（用于语音问答，含方言信息）
     */
    public static QaResponse fromVoiceEntity(QaRecord record, String dialect) {
        QaResponse response = fromEntity(record);
        response.setDialect(dialect);
        return response;
    }

    /**
     * 解析 JSON 格式的引用来源
     */
    private static List<SourceInfo> parseSources(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, String>> rawList = mapper.readValue(
                    sourcesJson, new TypeReference<List<Map<String, String>>>() {});
            return rawList.stream()
                    .map(m -> SourceInfo.builder()
                            .title(m.getOrDefault("title", ""))
                            .category(m.getOrDefault("category", ""))
                            .build())
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
