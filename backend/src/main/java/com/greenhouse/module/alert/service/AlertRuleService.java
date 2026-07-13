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
     * 创建预警规则
     */
    @Transactional
    public AlertRuleResponse createRule(Long userId, AlertRuleRequest request) {
        Greenhouse greenhouse = greenhouseRepository.findById(request.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));
        if (!greenhouse.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.GREENHOUSE_ACCESS_DENIED);
        }

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

        rule = ruleRepository.save(rule);
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
     * 更新预警规则
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

        rule.setSensorType(request.getSensorType());
        rule.setRuleType(AlertRule.RuleType.valueOf(request.getRuleType()));
        rule.setConditionJson(request.getConditionJson());
        rule.setAlertLevel(AlertRule.AlertLevel.valueOf(request.getAlertLevel()));
        rule.setSceneId(request.getSceneId());
        rule.setGroupId(request.getGroupId());
        rule.setEnabled(request.getEnabled());

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
}
