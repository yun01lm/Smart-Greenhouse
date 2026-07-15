package com.greenhouse.repository;

import com.greenhouse.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库文档 Repository
 * <p>
 * 提供文档元数据的 CRUD 操作，支持分页查询和分类筛选。
 * </p>
 */
@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    /** 按分类分页查询 */
    Page<KnowledgeDocument> findByCategory(String category, Pageable pageable);

    /** 按标题模糊搜索（分页） */
    Page<KnowledgeDocument> findByTitleContaining(String keyword, Pageable pageable);

    /** 按分类+标题模糊搜索（分页） */
    Page<KnowledgeDocument> findByTitleContainingAndCategory(String keyword, String category, Pageable pageable);

    /** 查询所有不重复的分类 */
    @Query("SELECT DISTINCT d.category FROM KnowledgeDocument d WHERE d.category IS NOT NULL")
    List<String> findDistinctCategories();

    /** 查询已向量化的文档 */
    List<KnowledgeDocument> findByVectorIndexedTrue();

    /** 查询未向量化的文档 */
    List<KnowledgeDocument> findByVectorIndexedFalse();

    /** 按分类统计文档数量 */
    long countByCategory(String category);
}
