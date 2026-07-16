package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统审计日志
 * <p>
 * 记录关键用户操作，用于安全审计和问题追踪。
 * 通过 AOP 切面 {@link com.greenhouse.security.aop.AuditAspect} 自动记录。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作用户ID */
    @Column(name = "user_id")
    private Long userId;

    /** 操作用户名 */
    @Column(name = "username", length = 50)
    private String username;

    /** 操作类型：LOGIN/LOGOUT/CREATE/UPDATE/DELETE/CONTROL/DIAGNOSIS/QA/UPLOAD */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    /** 操作目标（如设备ID、大棚ID等） */
    @Column(name = "target", length = 200)
    private String target;

    /** 操作详情（JSON或文本描述） */
    @Column(name = "\"detail\"", columnDefinition = "TEXT")
    private String detail;

    /** 请求IP地址 */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /** HTTP方法 */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /** 请求URI */
    @Column(name = "request_uri", length = 500)
    private String requestUri;

    /** 操作结果：SUCCESS/FAILURE */
    @Column(name = "result", nullable = false, length = 10)
    @Builder.Default
    private String result = "SUCCESS";

    /** 失败原因 */
    @Column(name = "fail_reason", length = 500)
    private String failReason;

    /** 操作耗时（毫秒） */
    @Column(name = "elapsed_ms")
    private Long elapsedMs;

    /** 操作时间 */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
