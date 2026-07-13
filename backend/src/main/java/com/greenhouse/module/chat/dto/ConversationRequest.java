package com.greenhouse.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建对话请求
 */
@Data
public class ConversationRequest {

    /** 专家ID */
    @NotNull(message = "专家ID不能为空")
    private Long expertId;

    /** 大棚ID */
    @NotNull(message = "大棚ID不能为空")
    private Long greenhouseId;

    /** 咨询主题 */
    @NotBlank(message = "咨询主题不能为空")
    private String subject;

    /** 关联诊断记录ID（可选，从诊断页面发起求助时附带） */
    private Long diagnosticId;
}
