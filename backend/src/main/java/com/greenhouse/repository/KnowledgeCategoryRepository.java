package com.greenhouse.repository;

import com.greenhouse.entity.KnowledgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识库分类 Repository
 */
@Repository
public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategory, Long> {

    /** 按 ID 升序查询全部分类 */
    List<KnowledgeCategory> findAllByOrderByIdAsc();

    /** 分类名称是否已存在 */
    boolean existsByName(String name);

    /** 分类名称是否已被其他分类占用（排除自身） */
    boolean existsByNameAndIdNot(String name, Long id);

    /** 按名称查询 */
    Optional<KnowledgeCategory> findByName(String name);
}
