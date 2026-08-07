package com.greenhouse.module.qa.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.entity.QaRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    /** 问题内容（完整） */
    private String question;

    /** 回答内容（完整，供前端恢复对话） */
    private String answer;

    /** 输入类型 */
    private String inputType;

    /** 语音识别引擎 */
    private String asrEngine;

    /** 引用来源（结构化数组） */
    private List<QaResponse.SourceInfo> sources;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 从实体转换（解析 sources JSON）
     */
    public static QaHistoryItem fromEntity(QaRecord record) {
        List<QaResponse.SourceInfo> sourceList = parseSources(record.getSources());

        return QaHistoryItem.builder()
                .id(record.getId())
                .question(record.getQuestion())
                .answer(record.getAnswer())
                .inputType(record.getInputType() != null ? record.getInputType().name() : null)
                .asrEngine(record.getAsrEngine())
                .sources(sourceList)
                .createdAt(record.getCreatedAt())
                .build();
    }

    /**
     * 解析来源 JSON（非法 JSON 时返回空列表）
     */
    private static List<QaResponse.SourceInfo> parseSources(String sourcesJson) {
        List<QaResponse.SourceInfo> result = new ArrayList<>();
        if (sourcesJson == null || sourcesJson.isBlank()) return result;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(sourcesJson);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    result.add(QaResponse.SourceInfo.builder()
                            .title(node.has("title") ? node.get("title").asText() : "")
                            .category(node.has("category") ? node.get("category").asText() : "")
                            .build());
                }
            }
        } catch (Exception e) {
            // 来源解析失败不阻塞历史返回
        }
        return result;
    }
}