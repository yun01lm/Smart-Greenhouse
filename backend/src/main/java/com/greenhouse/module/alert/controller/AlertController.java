package com.greenhouse.module.alert.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.common.PageResult;
import com.greenhouse.entity.User;
import com.greenhouse.repository.UserRepository;
import com.greenhouse.entity.Alert;
import com.greenhouse.module.alert.dto.AlertResponse;
import com.greenhouse.module.alert.dto.AlertRuleRequest;
import com.greenhouse.module.alert.dto.AlertRuleResponse;
import com.greenhouse.module.alert.dto.ThresholdRequest;
import com.greenhouse.module.alert.dto.ThresholdResponse;
import com.greenhouse.module.alert.service.AlertRuleService;
import com.greenhouse.module.alert.service.AlertService;
import com.greenhouse.module.alert.service.AlertThresholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 预警管理 API
 * <p>
 * 路径前缀：/api/v1/alerts
 * 遵循 Controller→Service→Repository 分层：Controller 仅做参数校验和结果转换，
 * 所有业务逻辑和 Repository 操作均在 AlertService / AlertRuleService / AlertThresholdService 中。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final AlertRuleService ruleService;
    private final AlertThresholdService thresholdService;
    private final UserRepository userRepository;

    // ===== 告警记录 =====

    /**
     * 告警列表（分页）
     * GET /api/v1/alerts?greenhouseId=1&page=0&size=20&level=WARNING
     */
    @GetMapping
    public ApiResponse<PageResult<AlertResponse>> list(
            @RequestParam Long greenhouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level) {

        Alert.AlertLevel alertLevel = (level != null && !level.isEmpty())
                ? Alert.AlertLevel.valueOf(level) : null;

        Page<Alert> alertPage = alertService.listAlerts(greenhouseId, alertLevel, page, size);
        String ghName = alertService.getGreenhouseName(greenhouseId);

        List<AlertResponse> list = alertPage.getContent().stream()
                .map(a -> AlertResponse.fromEntity(a, ghName))
                .toList();

        return ApiResponse.success(PageResult.of(list, alertPage.getTotalElements(), page, size));
    }

    /**
     * 标记告警已读
     * PUT /api/v1/alerts/{id}/read
     */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        alertService.markAsRead(id);
        return ApiResponse.success("已标记为已读", null);
    }

    /**
     * 标记告警已处理
     * PUT /api/v1/alerts/{id}/handle
     */
    @PutMapping("/{id}/handle")
    public ApiResponse<Void> markHandled(@PathVariable Long id) {
        alertService.markAsHandled(id);
        return ApiResponse.success("已标记为已处理", null);
    }

    /**
     * 获取未读告警数量
     * GET /api/v1/alerts/unread-count?greenhouseId=1
     */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Object>> unreadCount(@RequestParam Long greenhouseId) {
        return ApiResponse.success(alertService.getUnreadCount(greenhouseId));
    }

    // ===== 预警规则 =====

    /**
     * 规则列表（OWNER 自己的大棚 / WORKER 被授权大棚）
     * GET /api/v1/alerts/rules?greenhouseId=1（可选，缺省返回用户全部可见大棚的规则）
     */
    @GetMapping("/rules")
    public ApiResponse<List<AlertRuleResponse>> listRules(
            @RequestParam(required = false) Long greenhouseId,
            @RequestParam(required = false) Long ownerId) {
        Long userId = resolveUserId(ownerId);
        return ApiResponse.success(ruleService.listRulesForUser(userId, greenhouseId));
    }

    /**
     * 创建规则
     * POST /api/v1/alerts/rules
     */
    @PostMapping("/rules")
    public ApiResponse<AlertRuleResponse> createRule(@Valid @RequestBody AlertRuleRequest request,
                                                       @RequestParam(required = false) Long ownerId) {
        Long userId = resolveUserId(ownerId);
        return ApiResponse.success("规则创建成功", ruleService.createRule(userId, request));
    }

    /**
     * 更新规则
     * PUT /api/v1/alerts/rules/{id}
     */
    @PutMapping("/rules/{id}")
    public ApiResponse<AlertRuleResponse> updateRule(@PathVariable Long id,
                                                      @Valid @RequestBody AlertRuleRequest request,
                                                      @RequestParam(required = false) Long ownerId) {
        Long userId = resolveUserId(ownerId);
        return ApiResponse.success("规则更新成功", ruleService.updateRule(userId, id, request));
    }

    /**
     * 删除规则
     * DELETE /api/v1/alerts/rules/{id}
     */
    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id,
                                           @RequestParam(required = false) Long ownerId) {
        Long userId = resolveUserId(ownerId);
        ruleService.deleteRule(userId, id);
        return ApiResponse.success("规则已删除", null);
    }

    // ===== 自定义阈值 =====

    /**
     * 自定义阈值列表
     * GET /api/v1/alerts/thresholds?greenhouseId=1
     */
    @GetMapping("/thresholds")
    public ApiResponse<List<ThresholdResponse>> listThresholds(
            @RequestParam(required = false) Long greenhouseId,
            @RequestParam(required = false) Long ownerId) {
        Long userId = resolveUserId(ownerId);
        return ApiResponse.success(thresholdService.listThresholds(userId, greenhouseId));
    }

    /**
     * 设置自定义阈值
     * POST /api/v1/alerts/thresholds
     */
    @PostMapping("/thresholds")
    public ApiResponse<ThresholdResponse> setThreshold(@Valid @RequestBody ThresholdRequest request,
                                                         @RequestParam(required = false) Long ownerId) {
        Long userId = resolveUserId(ownerId);
        return ApiResponse.success("阈值设置成功", thresholdService.setThreshold(userId, request));
    }

    /**
     * 更新自定义阈值
     * PUT /api/v1/alerts/thresholds/{id}
     */
    @PutMapping("/thresholds/{id}")
    public ApiResponse<ThresholdResponse> updateThreshold(@PathVariable Long id,
                                                           @Valid @RequestBody ThresholdRequest request,
                                                           @RequestParam(required = false) Long ownerId) {
        Long userId = resolveUserId(ownerId);
        return ApiResponse.success("阈值更新成功", thresholdService.setThreshold(userId, request));
    }

    /**
     * 删除自定义阈值
     * DELETE /api/v1/alerts/thresholds/{id}
     */
    @DeleteMapping("/thresholds/{id}")
    public ApiResponse<Void> deleteThreshold(@PathVariable Long id,
                                                 @RequestParam(required = false) Long ownerId) {
        Long userId = resolveUserId(ownerId);
        thresholdService.deleteThreshold(userId, id);
        return ApiResponse.success("阈值已删除", null);
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    /**
     * 解析目标用户：默认当前登录用户；ADMIN 可携带 ownerId 代查棚主视角数据（R10）。
     * 非 ADMIN 携带 ownerId 一律拒绝，防止越权。
     */
    private Long resolveUserId(Long ownerId) {
        if (ownerId == null) {
            return getCurrentUserId();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User.Role role = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(x -> x.startsWith("ROLE_"))
                .map(x -> User.Role.valueOf(x.replace("ROLE_", "")))
                .findFirst().orElse(null);
        if (role != User.Role.ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (owner.getRole() != User.Role.OWNER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return ownerId;
    }
}
