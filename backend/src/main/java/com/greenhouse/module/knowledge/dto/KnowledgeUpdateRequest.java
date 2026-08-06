package com.greenhouse.module.knowledge.dto;

import lombok.Data;

/**
 * 知识库文档标记信息更新请求
 * <p>
 * 仅允许更新文档编号/标题/分类/简介等元数据，文件内容与向量数据不受影响。
 * 字段为空(null)表示不修改；编号或标题传空字符串会触发参数校验。
 * </p>
 */
@Data
public class KnowledgeUpdateRequest {

    /** 文档编号（业务编号，唯一） */
    private String docNo;

    /** 文档标题 */
    private String title;

    /** 文档分类 */
    private String category;

    /** 文档简介/内容摘要 */
    private String description;
}
