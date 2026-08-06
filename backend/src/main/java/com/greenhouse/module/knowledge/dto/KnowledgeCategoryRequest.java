package com.greenhouse.module.knowledge.dto;

import lombok.Data;

/**
 * 知识库分类新增/更新请求
 */
@Data
public class KnowledgeCategoryRequest {

    /** 分类名称（唯一，必填） */
    private String name;

    /** 分类说明（可选） */
    private String description;
}
