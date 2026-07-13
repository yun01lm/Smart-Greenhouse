package com.greenhouse.repository;

import com.greenhouse.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 预警记录数据访问层
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /** 按大棚查询告警（分页，按时间倒序） */
    Page<Alert> findByGreenhouseIdOrderByCreatedAtDesc(Long greenhouseId, Pageable pageable);

    /** 按大棚和级别查询 */
    Page<Alert> findByGreenhouseIdAndLevelOrderByCreatedAtDesc(
            Long greenhouseId, Alert.AlertLevel level, Pageable pageable);

    /** 按大棚查询未读告警 */
    List<Alert> findByGreenhouseIdAndReadStatusFalse(Long greenhouseId);

    /** 统计大棚未读告警数 */
    long countByGreenhouseIdAndReadStatusFalse(Long greenhouseId);
}
