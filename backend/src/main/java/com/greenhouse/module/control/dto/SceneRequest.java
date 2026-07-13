package com.greenhouse.module.control.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 场景创建/更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SceneRequest {

    /** 场景名称 */
    @NotBlank(message = "场景名称不能为空")
    private String name;

    /** 场景描述 */
    private String description;

    /**
     * 执行动作列表
     * 每个动作：{"deviceId": 1, "action": "ON"}
     */
    @NotEmpty(message = "动作列表不能为空")
    private List<SceneAction> actions;

    /** 单个控制动作 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneAction {
        @NotNull(message = "设备ID不能为空")
        private Long deviceId;

        @NotBlank(message = "动作不能为空")
        private String action;  // ON / OFF
    }
}
