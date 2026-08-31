package com.greenhouse.repository;

import com.greenhouse.entity.HealthAssessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 健康评估数据访问层
 */
@Repository
public interface HealthAssessmentRepository extends JpaRepository<HealthAssessment, Long> {

    /**
     * 查询大棚最新一次评估
     */
    Optional<HealthAssessment> findTopByGreenhouseIdOrderByCreatedAtDesc(Long greenhouseId);

    /**
     * 查询大棚在时间范围内的评估历史（分页，时间倒序）
     */
    Page<HealthAssessment> findByGreenhouseIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long greenhouseId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 查询大棚最近24小时内的评估记录数
     */
    @Query("SELECT COUNT(h) FROM HealthAssessment h WHERE h.greenhouseId = :greenhouseId " +
            "AND h.createdAt >= :since")
    long countRecentByGreenhouseId(@Param("greenhouseId") Long greenhouseId,
                                    @Param("since") LocalDateTime since);


    void deleteByGreenhouseId(Long greenhouseId);
}
