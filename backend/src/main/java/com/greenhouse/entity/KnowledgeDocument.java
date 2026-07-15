package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档实体
 * <p>
 * 对应数据库 knowledge_documents 表。
 * 存储知识库文档的元数据（MySQL），向量数据仍存储在 Chroma 中。
 * 实现"MySQL 管元数据，Chroma 管向量"的架构。
 * </p>
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>title：文档标题</li>
 *   <li>category：文档分类（如"病虫害防治"/"栽培技术"/"土壤管理"）</li>
 *   <li>filePath：文件存储路径</li>
 *   <li>fileType：文件类型（pdf/txt/md）</li>
 *   <li>fileSize：文件大小（字节）</li>
 *   <li>chunkCount：文档切分后的文本块数量</li>
 *   <li>vectorIndexed：是否已向量化并存入 Chroma</li>
 *   <li>indexedAt：向量化完成时间</li>
 * </ul>
 */
@Entity
@Table(name = "knowledge_documents", indexes = {
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_vector_indexed", columnList = "vector_indexed")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 文档标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 文档分类 */
    @Column(length = 100)
    private String category;

    /** 文件存储路径 */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /** 文件类型：pdf / txt / md */
    @Column(name = "file_type", length = 20)
    private String fileType;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    /** 文本块数量 */
    @Column(name = "chunk_count")
    @Builder.Default
    private Integer chunkCount = 0;

    /** 是否已向量化 */
    @Column(name = "vector_indexed")
    @Builder.Default
    private Boolean vectorIndexed = false;

    /** 向量化完成时间 */
    @Column(name = "indexed_at")
    private LocalDateTime indexedAt;

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
