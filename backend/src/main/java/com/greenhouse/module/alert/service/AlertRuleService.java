package com.greenhouse.module.alert.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.AlertRule;
import com.greenhouse.entity.EmployeePermission;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.module.alert.dto.AlertRuleRequest;
import com.greenhouse.module.alert.dto.AlertRuleResponse;
import com.greenhouse.repository.AlertRuleRepository;
import com.greenhouse.repository.EmployeePermissionRepository;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * 预警规则管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository ruleRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final UserRepository userRepository;
    private final EmployeePermissionRepository permissionRepository;

    private static final long MAX_RULES_PER_GREENHOUSE = 50;

    /**
     * 创建预警规则（OWNER 专用，校验大棚所有权）
     */
    @Transactional
    public AlertRuleResponse createRule(Long userId, AlertRuleRequest request) {
        Greenhouse greenhouse = greenhouseRepository.findById(request.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        AlertRule rule = doCreateRule(request);
        log.info("预警规则创建成功: id={}, greenhouseId={}, sensorType={}, type={}",
                rule.getId(), rule.getGreenhouseId(), rule.getSensorType(), rule.getRuleType());

        return AlertRuleResponse.fromEntity(rule);
    }

    /**
     * 查询大棚预警规则列表
     */
    public List<AlertRuleResponse> listRules(Long greenhouseId) {
        List<AlertRule> rules = ruleRepository.findByGreenhouseId(greenhouseId);
        return rules.stream().map(AlertRuleResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * 查询当前用户可见的预警规则（R8 权限收口）
     * <p>OWNER 只能看自己大棚的规则；WORKER 只能看被授权大棚的规则；其他角色返回空列表。
     * greenhouseId 为空时返回用户全部可见大棚的规则，便于前端「全部」筛选。</p>
     */
    public List<AlertRuleResponse> listRulesForUser(Long userId, Long greenhouseId) {
        if (greenhouseId != null) {
            assertGreenhouseAccess(userId, greenhouseId);
            return listRules(greenhouseId);
        }
        List<Long> accessibleIds = accessibleGreenhouseIds(userId);
        if (accessibleIds.isEmpty()) {
            return List.of();
        }
        return ruleRepository.findByGreenhouseIdIn(accessibleIds).stream()
                .map(AlertRuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 校验用户对大棚的访问权限：OWNER 本人大棚 / WORKER 被授权大棚
     */
    private void assertGreenhouseAccess(Long userId, Long greenhouseId) {
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        boolean allowed = (user.getRole() == User.Role.OWNER && greenhouse.getOwnerId().equals(userId))
                || (user.getRole() == User.Role.WORKER
                    && permissionRepository.existsByEmployeeIdAndGreenhouseId(userId, greenhouseId));
        if (!allowed) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }
    }

    /**
     * 获取用户可见的大棚 ID 列表（OWNER → 自己的大棚；WORKER → 被授权的大棚）
     */
    private List<Long> accessibleGreenhouseIds(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (user.getRole() == User.Role.OWNER) {
            return greenhouseRepository.findByOwnerId(userId).stream()
                    .map(Greenhouse::getId)
                    .toList();
        }
        if (user.getRole() == User.Role.WORKER) {
            return permissionRepository.findByEmployeeId(userId).stream()
                    .map(EmployeePermission::getGreenhouseId)
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    /**
     * 更新预警规则（OWNER 专用，校验大棚所有权）
     */
    @Transactional
    public AlertRuleResponse updateRule(Long userId, Long ruleId, AlertRuleRequest request) {
        AlertRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "预警规则不存在"));

        Greenhouse greenhouse = greenhouseRepository.findById(rule.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        applyRuleFields(rule, request);
        rule = ruleRepository.save(rule);
        log.info("预警规则更新成功: id={}", rule.getId());

        return AlertRuleResponse.fromEntity(rule);
    }

    /**
     * 删除预警规则
     */
    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        AlertRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "预警规则不存在"));

        Greenhouse greenhouse = greenhouseRepository.findById(rule.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

        ruleRepository.delete(rule);
        log.info("预警规则删除成功: id={}", ruleId);
    }

    // ===== 公共方法 =====

    /**
     * 校验数量上限 + 构建并保存规则实体（createRule 和 createRuleAdmin 共用）
     */
    private AlertRule doCreateRule(AlertRuleRequest request) {
        if (ruleRepository.countByGreenhouseId(request.getGreenhouseId()) >= MAX_RULES_PER_GREENHOUSE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "预警规则数量已达上限(" + MAX_RULES_PER_GREENHOUSE + "条)");
        }

        AlertRule rule = AlertRule.builder()
                .greenhouseId(request.getGreenhouseId())
                .groupId(request.getGroupId())
                .sensorType(request.getSensorType())
                .ruleType(AlertRule.RuleType.valueOf(request.getRuleType()))
                .conditionJson(request.getConditionJson())
                .alertLevel(AlertRule.AlertLevel.valueOf(request.getAlertLevel()))
                .sceneId(request.getSceneId())
                .enabled(request.getEnabled())
                .build();

        return ruleRepository.save(rule);
    }

    /**
     * 应用请求字段到已有规则实体（updateRule 和 updateRuleAdmin 共用）
     */
    private void applyRuleFields(AlertRule rule, AlertRuleRequest request) {
        rule.setSensorType(request.getSensorType());
        rule.setRuleType(AlertRule.RuleType.valueOf(request.getRuleType()));
        rule.setConditionJson(request.getConditionJson());
        rule.setAlertLevel(AlertRule.AlertLevel.valueOf(request.getAlertLevel()));
        rule.setSceneId(request.getSceneId());
        rule.setGroupId(request.getGroupId());
        rule.setEnabled(request.getEnabled());
    }

    // ===== ADMIN 专用方法（绕过所有权校验） =====

    /**
     * 查询所有预警规则（ADMIN 专用）��
     * <p>注意：当前未分页，适用于预警规则总量可控（每大棚最多50条）的场景。</p>
     */
    public List<AlertRuleResponse> listAllRules() {
        List<AlertRule> rules = ruleRepository.findAll();
        return rules.stream().map(AlertRuleResponse::fromEntity).collect(toList());
    }

    /**
     * 按大棚查询规则（ADMIN 专用，不校验所有权）
     */
    public List<AlertRuleResponse> listRulesByGreenhouse(Long greenhouseId) {
        return listRules(greenhouseId);
    }

    /**
     * 创建预警规则（ADMIN 专用，绕过所有权校验）
     */
    @Transactional
    public AlertRuleResponse createRuleAdmin(AlertRuleRequest request) {
        greenhouseRepository.findById(request.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        AlertRule rule = doCreateRule(request);
        log.info("[ADMIN] 预警规则创建成功: id={}, greenhouseId={}, sensorType={}",
                rule.getId(), rule.getGreenhouseId(), rule.getSensorType());

        return AlertRuleResponse.fromEntity(rule);
    }

    /**
     * 更新预警规则（ADMIN 专用，绕过所有权校验）
     */
    @Transactional
    public AlertRuleResponse updateRuleAdmin(Long ruleId, AlertRuleRequest request) {
        AlertRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "预警规则不存在"));

        applyRuleFields(rule, request);
        rule = ruleRepository.save(rule);
        log.info("[ADMIN] 预警规则更新成功: id={}", rule.getId());

        return AlertRuleResponse.fromEntity(rule);
    }

    /**
     * 删除预警规则（ADMIN 专用，绕过所有权校验）
     */
    @Transactional
    public void deleteRuleAdmin(Long ruleId) {
        AlertRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "预警规则不存在"));

        ruleRepository.delete(rule);
        log.info("[ADMIN] 预警规则删除成功: id={}", ruleId);
    }
}
