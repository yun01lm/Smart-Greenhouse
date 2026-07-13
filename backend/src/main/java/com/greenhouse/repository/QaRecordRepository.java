package com.greenhouse.repository;

import com.greenhouse.entity.QaRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AI 问答记录 Repository
 */
@Repository
public interface QaRecordRepository extends JpaRepository<QaRecord, Long> {

    /**
     * 按用户ID分页查询问答历史（按时间倒序）
     */
    Page<QaRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
