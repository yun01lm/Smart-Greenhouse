package com.greenhouse.module.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文字问答请求
 */
@Data
public class QaAskRequest {

    /** 问题内容 */
    @NotBlank(message = "问题内容不能为空")
    @Size(max = 1000, message = "问题内容不能超过1000字")
    private String question;

    /** 关联大棚ID（可选，用于关联大棚环境上下文） */
    private Long greenhouseId;
}
