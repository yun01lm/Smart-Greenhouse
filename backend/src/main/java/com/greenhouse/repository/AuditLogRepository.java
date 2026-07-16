package com.greenhouse.repository;

import com.greenhouse.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志 Repository
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** 按用户ID查询 */
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 按操作类型查询 */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    /** 按时间范围查询 */
    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end, Pageable pageable);

    /** 按用户+操作类型查询 */
    Page<AuditLog> findByUserIdAndActionOrderByCreatedAtDesc(
            Long userId, String action, Pageable pageable);

    /** 按结果查询 */
    Page<AuditLog> findByResultOrderByCreatedAtDesc(String result, Pageable pageable);
}
