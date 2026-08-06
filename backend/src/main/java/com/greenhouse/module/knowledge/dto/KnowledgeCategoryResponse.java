package com.greenhouse.module.knowledge.dto;

import com.greenhouse.entity.KnowledgeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库分类响应（含文档数）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeCategoryResponse {

    private Long id;
    private String name;
    private String description;

    /** 该分类下文档数量 */
    private Long docCount;

    public static KnowledgeCategoryResponse from(KnowledgeCategory category, Long docCount) {
        return KnowledgeCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .docCount(docCount)
                .build();
    }
}
