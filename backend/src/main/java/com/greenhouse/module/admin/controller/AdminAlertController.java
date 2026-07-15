package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.alert.dto.AlertRuleRequest;
import com.greenhouse.module.alert.dto.AlertRuleResponse;
import com.greenhouse.module.alert.dto.ThresholdRequest;
import com.greenhouse.module.alert.dto.ThresholdResponse;
import com.greenhouse.module.alert.service.AlertRuleService;
import com.greenhouse.module.alert.service.AlertThresholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员预警配置 API
 * <p>
 * 路径前缀：/api/v1/admin/alerts，仅 ADMIN 角色可访问。
 * 提供全量预警规则和自定义阈值管理，绕过 OWNER 所有权校验。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/alerts")
@RequiredArgsConstructor
public class AdminAlertController {

    private final AlertRuleService ruleService;
    private final AlertThresholdService thresholdService;

    // ===== 预警规则管理（ADMIN 全量） =====

    /**
     * 查询所有预警规则（可选按大棚筛选）
     * GET /api/v1/admin/alerts/rules?greenhouseId=
     */
    @GetMapping("/rules")
    public ApiResponse<List<AlertRuleResponse>> listRules(
            @RequestParam(required = false) Long greenhouseId) {
        if (greenhouseId != null) {
            return ApiResponse.success(ruleService.listRulesByGreenhouse(greenhouseId));
        }
        return ApiResponse.success(ruleService.listAllRules());
    }

    /**
     * 创建预警规则
     * POST /api/v1/admin/alerts/rules
     */
    @PostMapping("/rules")
    public ApiResponse<AlertRuleResponse> createRule(@Valid @RequestBody AlertRuleRequest request) {
        log.info("[ADMIN] 创建预警规则: greenhouseId={}, sensorType={}, ruleType={}",
                request.getGreenhouseId(), request.getSensorType(), request.getRuleType());
        return ApiResponse.success("规则创建成功", ruleService.createRuleAdmin(request));
    }

    /**
     * 更新预警规则
     * PUT /api/v1/admin/alerts/rules/{id}
     */
    @PutMapping("/rules/{id}")
    public ApiResponse<AlertRuleResponse> updateRule(@PathVariable Long id,
                                                      @Valid @RequestBody AlertRuleRequest request) {
        log.info("[ADMIN] 更新预警规则: id={}", id);
        return ApiResponse.success("规则更新成功", ruleService.updateRuleAdmin(id, request));
    }

    /**
     * 删除预警规则
     * DELETE /api/v1/admin/alerts/rules/{id}
     */
    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        ruleService.deleteRuleAdmin(id);
        return ApiResponse.success("规则已删除", null);
    }

    // ===== 自定义阈值管理（ADMIN 全量） =====

    /**
     * 查询所有自定义阈值（可选按大棚筛选）
     * GET /api/v1/admin/alerts/thresholds?greenhouseId=
     */
    @GetMapping("/thresholds")
    public ApiResponse<List<ThresholdResponse>> listThresholds(
            @RequestParam(required = false) Long greenhouseId) {
        if (greenhouseId != null) {
            return ApiResponse.success(thresholdService.listThresholdsByGreenhouse(greenhouseId));
        }
        return ApiResponse.success(thresholdService.listAllThresholds());
    }

    /**
     * 删除自定义阈值
     * DELETE /api/v1/admin/alerts/thresholds/{id}
     */
    @DeleteMapping("/thresholds/{id}")
    public ApiResponse<Void> deleteThreshold(@PathVariable Long id) {
        thresholdService.deleteThresholdAdmin(id);
        return ApiResponse.success("阈值已删除", null);
    }
}
