package com.greenhouse.repository;

import com.greenhouse.entity.GrowthAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 长势评估记录数据访问层
 */
@Repository
public interface GrowthAssessmentRepository extends JpaRepository<GrowthAssessment, Long> {

    /** 查询大棚最新一次长势评估 */
    Optional<GrowthAssessment> findTopByGreenhouseIdOrderByCreatedAtDesc(Long greenhouseId);

    /** 按大棚分页查询长势历史 */
    Page<GrowthAssessment> findByGreenhouseIdOrderByCreatedAtDesc(Long greenhouseId, Pageable pageable);

    /** 按生长周期查询 */
    Page<GrowthAssessment> findByCropCycleIdOrderByCreatedAtDesc(Long cropCycleId, Pageable pageable);
}
