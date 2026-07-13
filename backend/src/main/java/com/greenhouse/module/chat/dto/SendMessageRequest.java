package com.greenhouse.module.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息请求
 */
@Data
public class SendMessageRequest {

    /** 对话ID */
    @NotNull(message = "对话ID不能为空")
    private Long conversationId;

    /** 消息类型 */
    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    /** 文字内容（TEXT时必填） */
    private String content;

    /** 文件路径（IMAGE/VIDEO时必填） */
    private String filePath;

    /** 快照数据（ENV_SNAPSHOT时使用） */
    private String snapshotData;
}
