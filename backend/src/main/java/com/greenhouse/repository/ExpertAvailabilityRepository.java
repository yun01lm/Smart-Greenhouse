package com.greenhouse.repository;

import com.greenhouse.entity.ExpertAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 专家在线状态 Repository
 */
@Repository
public interface ExpertAvailabilityRepository extends JpaRepository<ExpertAvailability, Long> {

    /**
     * 按专家ID查询
     */
    Optional<ExpertAvailability> findByExpertId(Long expertId);
}
