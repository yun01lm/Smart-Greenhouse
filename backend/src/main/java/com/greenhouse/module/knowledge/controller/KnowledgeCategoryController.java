package com.greenhouse.module.knowledge.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.knowledge.dto.KnowledgeCategoryRequest;
import com.greenhouse.module.knowledge.dto.KnowledgeCategoryResponse;
import com.greenhouse.module.knowledge.service.KnowledgeCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库分类管理 API
 * <p>
 * 路径前缀：/api/v1/knowledge/categories/managed
 * 权限：仅 ADMIN 可访问（/api/v1/knowledge/** 由 SecurityConfig 控制）
 * </p>
 */
@RestController
@RequestMapping("/api/v1/knowledge/categories/managed")
@RequiredArgsConstructor
public class KnowledgeCategoryController {

    private final KnowledgeCategoryService categoryService;

    /** 分类列表（含文档数） */
    @GetMapping
    public ApiResponse<List<KnowledgeCategoryResponse>> list() {
        return ApiResponse.success(categoryService.list());
    }

    /** 新增分类 */
    @PostMapping
    public ApiResponse<KnowledgeCategoryResponse> create(@RequestBody KnowledgeCategoryRequest request) {
        return ApiResponse.success("分类已创建", categoryService.create(request));
    }

    /** 重命名/编辑分类 */
    @PutMapping("/{id}")
    public ApiResponse<KnowledgeCategoryResponse> update(@PathVariable Long id,
                                                         @RequestBody KnowledgeCategoryRequest request) {
        return ApiResponse.success("分类已更新", categoryService.update(id, request));
    }

    /** 删除分类 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success("分类已删除", null);
    }
}
