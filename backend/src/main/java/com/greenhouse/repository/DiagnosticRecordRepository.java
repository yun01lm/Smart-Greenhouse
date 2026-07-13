package com.greenhouse.repository;

import com.greenhouse.entity.DiagnosticRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 诊断记录数据访问层
 */
@Repository
public interface DiagnosticRecordRepository extends JpaRepository<DiagnosticRecord, Long> {

    /** 按用户查询诊断历史（分页） */
    Page<DiagnosticRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 按大棚查询诊断记录 */
    List<DiagnosticRecord> findByGreenhouseIdOrderByCreatedAtDesc(Long greenhouseId);
}
