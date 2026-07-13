package com.greenhouse.module.greenhouse.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.User;
import com.greenhouse.module.greenhouse.dto.GreenhouseRequest;
import com.greenhouse.module.greenhouse.dto.GreenhouseResponse;
import com.greenhouse.module.greenhouse.dto.RegionStatsResponse;
import com.greenhouse.module.greenhouse.service.GreenhouseService;
import com.greenhouse.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 大棚管理 API
 */
@RestController
@RequestMapping("/api/v1/greenhouses")
@RequiredArgsConstructor
public class GreenhouseController {

    private final GreenhouseService greenhouseService;
    private final UserRepository userRepository;

    /**
     * 获取大棚列表（按角色过滤）
     * GET /api/v1/greenhouses
     */
    @GetMapping
    public ApiResponse<List<GreenhouseResponse>> list() {
        Long userId = getCurrentUserId();
        User.Role role = getCurrentUserRole();
        List<GreenhouseResponse> list = greenhouseService.listGreenhouses(userId, role);
        return ApiResponse.success(list);
    }

    /**
     * 获取大棚详情
     * GET /api/v1/greenhouses/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<GreenhouseResponse> get(@PathVariable Long id) {
        return ApiResponse.success(greenhouseService.getGreenhouse(id));
    }

    /**
     * 创建大棚（仅棚主）
     * POST /api/v1/greenhouses
     */
    @PostMapping
    public ApiResponse<GreenhouseResponse> create(@Valid @RequestBody GreenhouseRequest request) {
        Long userId = getCurrentUserId();
        GreenhouseResponse response = greenhouseService.createGreenhouse(userId, request);
        return ApiResponse.success("大棚创建成功", response);
    }

    /**
     * 更新大棚（仅棚主操作自己的大棚）
     * PUT /api/v1/greenhouses/{id}
     */
    @PutMapping("/{id}")
    public ApiResponse<GreenhouseResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody GreenhouseRequest request) {
        Long userId = getCurrentUserId();
        GreenhouseResponse response = greenhouseService.updateGreenhouse(userId, id, request);
        return ApiResponse.success("大棚更新成功", response);
    }

    /**
     * 删除大棚（仅棚主操作自己的大棚）
     * DELETE /api/v1/greenhouses/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        greenhouseService.deleteGreenhouse(userId, id);
        return ApiResponse.success("大棚删除成功", null);
    }

    /**
     * 地区分布统计（管理员功能）
     * GET /api/v1/greenhouses/regions
     */
    @GetMapping("/regions")
    public ApiResponse<List<RegionStatsResponse>> regions() {
        return ApiResponse.success(greenhouseService.getRegionStats());
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
        // 去掉 ROLE_ 前缀
        return User.Role.valueOf(roleStr.replace("ROLE_", ""));
    }
}
