package com.greenhouse.repository;

import com.greenhouse.entity.QaRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * AI 问答记录 Repository
 */
@Repository
public interface QaRecordRepository extends JpaRepository<QaRecord, Long> {

    /**
     * 按用户ID分页查询问答历史（按时间倒序）
     */
    Page<QaRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 按用户ID + 创建时间范围分页查询问答历史（按时间倒序）
     * <p>用于按日期查询：start = 当天 00:00，end = 次日 00:00（左闭右开）</p>
     */
    Page<QaRecord> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}