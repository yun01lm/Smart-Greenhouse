package com.greenhouse.repository;

import com.greenhouse.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 预警规则数据访问层
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /** 按大棚查询所有规则 */
    List<AlertRule> findByGreenhouseId(Long greenhouseId);

    /** 按大棚和传感器类型查询启用的规则 */
    List<AlertRule> findByGreenhouseIdAndSensorTypeAndEnabledTrue(Long greenhouseId, String sensorType);

    /** 按大棚查询所有启用的规则 */
    List<AlertRule> findByGreenhouseIdAndEnabledTrue(Long greenhouseId);

    /** 统计大棚下规则数量 */
    long countByGreenhouseId(Long greenhouseId);
    /** 按规则类型查询所有启用的规则 */
    List<AlertRule> findByRuleTypeAndEnabledTrue(AlertRule.RuleType ruleType);

}
