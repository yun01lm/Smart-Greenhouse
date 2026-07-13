package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 专家在线状态实体
 * <p>
 * 对应 DB 第 24 号表 expert_availability。
 * 管理专家的在线状态和并发咨询数。
 * </p>
 */
@Entity
@Table(name = "expert_availability")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 专家ID */
    @Column(name = "expert_id", nullable = false, unique = true)
    private Long expertId;

    /** 是否在线 */
    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private Integer isOnline = 0;

    /** 最后活跃时间 */
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    /** 最大同时咨询数 */
    @Column(name = "max_concurrent", nullable = false)
    @Builder.Default
    private Integer maxConcurrent = 5;
}
