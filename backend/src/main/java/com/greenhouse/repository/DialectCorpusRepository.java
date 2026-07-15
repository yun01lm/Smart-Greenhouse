package com.greenhouse.repository;

import com.greenhouse.entity.DialectCorpus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 方言语料数据访问层
 */
@Repository
public interface DialectCorpusRepository extends JpaRepository<DialectCorpus, Long> {

    /** 按方言类型分页查询 */
    Page<DialectCorpus> findByDialect(String dialect, Pageable pageable);

    /** 按方言类型和关键词搜索（标注文本模糊匹配） */
    Page<DialectCorpus> findByDialectAndAnnotationTextContaining(
            String dialect, String keyword, Pageable pageable);

    /** 全文搜索标注文本（不区分方言） */
    Page<DialectCorpus> findByAnnotationTextContaining(String keyword, Pageable pageable);

    /** 获取所有方言类型（去重） */
    @org.springframework.data.jpa.repository.Query(
            "SELECT DISTINCT c.dialect FROM DialectCorpus c ORDER BY c.dialect")
    List<String> findDistinctDialects();
}
