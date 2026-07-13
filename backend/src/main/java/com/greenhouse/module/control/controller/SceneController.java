package com.greenhouse.module.control.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.User;
import com.greenhouse.module.control.dto.ControlLogResponse;
import com.greenhouse.module.control.dto.SceneRequest;
import com.greenhouse.module.control.dto.SceneResponse;
import com.greenhouse.module.control.service.SceneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场景联动 API
 * <p>
 * 路径前缀：/api/v1/control/scenes
 * </p>
 */
@RestController
@RequestMapping("/api/v1/control/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    /**
     * 场景列表
     * GET /api/v1/control/scenes?greenhouseId=1
     */
    @GetMapping
    public ApiResponse<List<SceneResponse>> list(@RequestParam Long greenhouseId) {
        return ApiResponse.success(sceneService.listScenes(greenhouseId));
    }

    /**
     * 创建场景
     * POST /api/v1/control/scenes?greenhouseId=1
     */
    @PostMapping
    public ApiResponse<SceneResponse> create(@RequestParam Long greenhouseId,
                                              @Valid @RequestBody SceneRequest request) {
        Long userId = getCurrentUserId();
        SceneResponse response = sceneService.createScene(userId, greenhouseId, request);
        return ApiResponse.success("场景创建成功", response);
    }

    /**
     * 更新场景
     * PUT /api/v1/control/scenes/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<SceneResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody SceneRequest request) {
        Long userId = getCurrentUserId();
        SceneResponse response = sceneService.updateScene(userId, id, request);
        return ApiResponse.success("场景更新成功", response);
    }

    /**
     * 删除场景
     * DELETE /api/v1/control/scenes/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        sceneService.deleteScene(userId, id);
        return ApiResponse.success("场景已删除", null);
    }

    /**
     * 执行场景（手动触发）
     * POST /api/v1/control/scenes/{id}/execute
     */
    @PostMapping("/{id}/execute")
    public ApiResponse<List<ControlLogResponse>> execute(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        User.Role role = getCurrentUserRole();
        List<ControlLogResponse> results = sceneService.executeScene(userId, role, id);
        return ApiResponse.success("场景执行完成", results);
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    private User.Role getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String roleStr = auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_OWNER");
        return User.Role.valueOf(roleStr.replace("ROLE_", ""));
    }
}
