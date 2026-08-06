package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库分类实体
 * <p>
 * 对应 knowledge_categories 表，管理知识库文档的分类（正式分类管理）。
 * 文档表的 category 字段保存分类名称字符串，与分类表名称保持一致；
 * 重命名分类时由服务层级联更新文档及向量库元数据。
 * </p>
 */
@Entity
@Table(name = "knowledge_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 分类名称（唯一） */
    @Column(nullable = false, length = 100, unique = true)
    private String name;

    /** 分类说明 */
    @Column(length = 255)
    private String description;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
