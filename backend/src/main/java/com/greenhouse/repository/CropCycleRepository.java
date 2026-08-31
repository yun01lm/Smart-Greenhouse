package com.greenhouse.repository;

import com.greenhouse.entity.CropCycle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 作物生长周期 Repository
 */
@Repository
public interface CropCycleRepository extends JpaRepository<CropCycle, Long> {

    /**
     * 按大棚和状态查询（分页）
     */
    Page<CropCycle> findByGreenhouseIdAndStatus(Long greenhouseId, CropCycle.CycleStatus status, Pageable pageable);

    /**
     * 按大棚查询全部（分页）
     */
    Page<CropCycle> findByGreenhouseId(Long greenhouseId, Pageable pageable);

    /**
     * 查询大棚下进行中的周期（ACTIVE 优先置顶）
     */
    List<CropCycle> findByGreenhouseIdOrderByStatusAscCreatedAtDesc(Long greenhouseId);

    /**
     * 检查大棚是否已有进行中的周期
     */
    Optional<CropCycle> findTopByGreenhouseIdAndStatus(Long greenhouseId, CropCycle.CycleStatus status);


    void deleteByGreenhouseId(Long greenhouseId);
}
