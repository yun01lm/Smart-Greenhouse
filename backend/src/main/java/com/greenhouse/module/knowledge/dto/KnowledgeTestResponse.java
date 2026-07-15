package com.greenhouse.module.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库问答测试响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeTestResponse {

    /** 测试问题 */
    private String question;

    /** AI 生成的回答 */
    private String answer;

    /** 检索到的相关文档片段 */
    private List<RetrievedChunk> retrievedChunks;

    /** 响应时间（毫秒） */
    private Long responseTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievedChunk {
        private Long documentId;
        private String documentTitle;
        private String content;
        private Double score;
    }
}
