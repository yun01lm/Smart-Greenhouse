package com.greenhouse.module.knowledge.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.KnowledgeCategory;
import com.greenhouse.module.knowledge.dto.KnowledgeCategoryRequest;
import com.greenhouse.module.knowledge.dto.KnowledgeCategoryResponse;
import com.greenhouse.repository.KnowledgeCategoryRepository;
import com.greenhouse.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库分类管理服务（正式分类管理）
 * <p>
 * 分类为受管数据（knowledge_categories 表），支持新增/重命名/删除；
 * 重命名会级联更新文档分类与向量库元数据；删除前校验是否仍有文档引用。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeCategoryService {

    private final KnowledgeCategoryRepository categoryRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeService knowledgeService;

    /** 分类列表（含文档数） */
    public List<KnowledgeCategoryResponse> list() {
        return categoryRepository.findAllByOrderByIdAsc().stream()
                .map(cat -> KnowledgeCategoryResponse.from(cat, documentRepository.countByCategory(cat.getName())))
                .collect(Collectors.toList());
    }

    /** 新增分类 */
    @Transactional
    public KnowledgeCategoryResponse create(KnowledgeCategoryRequest request) {
        String name = normalizeName(request.getName());
        if (categoryRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类已存在：" + name);
        }
        KnowledgeCategory category = KnowledgeCategory.builder()
                .name(name)
                .description(trimToNull(request.getDescription(), 255, "分类说明"))
                .build();
        category = categoryRepository.save(category);
        log.info("知识库分类已新增: id={}, name={}", category.getId(), name);
        return KnowledgeCategoryResponse.from(category, 0L);
    }

    /** 重命名/编辑分类（级联更新文档与向量库元数据） */
    @Transactional
    public KnowledgeCategoryResponse update(Long id, KnowledgeCategoryRequest request) {
        KnowledgeCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "分类不存在"));
        String newName = normalizeName(request.getName());
        if (categoryRepository.existsByNameAndIdNot(newName, id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类已存在：" + newName);
        }
        String oldName = category.getName();
        category.setName(newName);
        category.setDescription(trimToNull(request.getDescription(), 255, "分类说明"));
        category = categoryRepository.save(category);

        // 名称变化时级联更新文档分类与向量库元数据
        if (!oldName.equals(newName)) {
            knowledgeService.renameDocumentCategory(oldName, newName);
        }
        log.info("知识库分类已更新: id={}, old={}, new={}", id, oldName, newName);
        return KnowledgeCategoryResponse.from(category, documentRepository.countByCategory(newName));
    }

    /** 删除分类（有文档引用时拒绝） */
    @Transactional
    public void delete(Long id) {
        KnowledgeCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "分类不存在"));
        long docCount = documentRepository.countByCategory(category.getName());
        if (docCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "该分类下还有 " + docCount + " 个文档，无法删除；请先调整这些文档的分类");
        }
        categoryRepository.delete(category);
        log.info("知识库分类已删除: id={}, name={}", id, category.getName());
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类名称不能为空");
        }
        String normalized = name.trim();
        if (normalized.length() > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类名称长度不能超过 100 字符");
        }
        return normalized;
    }

    private String trimToNull(String value, int maxLength, String field) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, field + "长度不能超过 " + maxLength + " 字符");
        }
        return trimmed;
    }
}
