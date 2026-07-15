package com.greenhouse.module.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库问答测试请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeTestRequest {

    /** 测试问题 */
    private String question;

    /** 检索 Top-K（默认 5） */
    @Builder.Default
    private Integer topK = 5;
}
