package com.greenhouse.module.control.dto;

import com.greenhouse.entity.Scene;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场景响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SceneResponse {

    private Long id;
    private String name;
    private String description;
    private Long greenhouseId;
    private List<SceneActionInfo> actions;
    private Boolean enabled;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SceneActionInfo {
        private Long deviceId;
        private String deviceName;
        private String action;
    }

    public static SceneResponse fromEntity(Scene scene, List<SceneActionInfo> actions) {
        return SceneResponse.builder()
                .id(scene.getId())
                .name(scene.getName())
                .description(scene.getDescription())
                .greenhouseId(scene.getGreenhouseId())
                .actions(actions)
                .enabled(scene.getEnabled())
                .createdAt(scene.getCreatedAt())
                .build();
    }
}
