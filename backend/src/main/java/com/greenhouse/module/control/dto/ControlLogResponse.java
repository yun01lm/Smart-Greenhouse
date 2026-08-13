package com.greenhouse.module.control.dto;

import com.greenhouse.entity.ControlLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 控制日志响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlLogResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long deviceId;
    private String deviceName;
    private String action;
    private String source;
    private Long sceneId;
    /** 触发场景名称（sceneId 有值时填充，便于前端展示"场景触发：XXX"） */
    private String sceneName;
    private Boolean success;
    private String failReason;
    private LocalDateTime createdAt;

    public static ControlLogResponse fromEntity(ControlLog log, String username, String deviceName) {
        return ControlLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(username)
                .deviceId(log.getDeviceId())
                .deviceName(deviceName)
                .action(log.getAction())
                .source(log.getSource())
                .sceneId(log.getSceneId())
                .success(log.getSuccess())
                .failReason(log.getFailReason())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
