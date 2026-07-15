package com.greenhouse.module.alert.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.AlertRule;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.module.alert.dto.AlertRuleRequest;
import com.greenhouse.module.alert.dto.AlertRuleResponse;
import com.greenhouse.repository.AlertRuleRepository;
import com.greenhouse.repository.GreenhouseRepository;
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
