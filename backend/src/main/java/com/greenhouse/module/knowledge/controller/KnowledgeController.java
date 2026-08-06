package com.greenhouse.module.knowledge.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.PageResult;
import com.greenhouse.module.knowledge.dto.KnowledgeDocumentResponse;
import com.greenhouse.module.knowledge.dto.KnowledgeTestRequest;
import com.greenhouse.module.knowledge.dto.KnowledgeTestResponse;
import com.greenhouse.module.knowledge.dto.KnowledgeUpdateRequest;
import com.greenhouse.module.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理 API
 * <p>
 * 路径前缀：/api/v1/knowledge
 * 权限：仅 ADMIN 可访问（由 SecurityConfig 控制）
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /**
     * 文档列表（分页 + 分类筛选 + 关键词搜索）
     * GET /api/v1/knowledge/documents?category=病虫害防治&keyword=番茄&page=1&size=10
     */
    @GetMapping("/documents")
    public ApiResponse<PageResult<KnowledgeDocumentResponse>> listDocuments(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<KnowledgeDocumentResponse> docPage = knowledgeService.listDocuments(category, keyword, page, size);

        return ApiResponse.success(PageResult.of(
                docPage.getContent(),
                docPage.getTotalElements(),
                page,
                size));
    }

    /**
     * 获取分类列表
     * GET /api/v1/knowledge/categories
     */
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(knowledgeService.getCategories());
    }

    /**
     * 上传文档
     * POST /api/v1/knowledge/documents
     */
    @PostMapping("/documents")
    public ApiResponse<KnowledgeDocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {

        KnowledgeDocumentResponse doc = knowledgeService.uploadDocument(file, title, category);
        return ApiResponse.success("文档上传成功，向量化处理中（可在列表中查看状态或稍后手动重新向量化）", doc);
    }

    /**
     * 触发向量化索引
     * POST /api/v1/knowledge/index
     */
    @PostMapping("/index")
    public ApiResponse<KnowledgeDocumentResponse> indexDocument(@RequestParam Long documentId) {
        KnowledgeDocumentResponse doc = knowledgeService.indexDocument(documentId);
        return ApiResponse.success("向量化完成，共 " + doc.getChunkCount() + " 个文本块", doc);
    }

    /**
     * 删除文档
     * DELETE /api/v1/knowledge/documents/{id}
     */
    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        knowledgeService.deleteDocument(id);
        return ApiResponse.success("文档已删除", null);
    }

    /**
     * 更新文档标记信息（编号/标题/分类/简介）
     * PUT /api/v1/knowledge/documents/{id}
     */
    @PutMapping("/documents/{id}")
    public ApiResponse<KnowledgeDocumentResponse> updateDocument(
            @PathVariable Long id,
            @RequestBody KnowledgeUpdateRequest request) {
        KnowledgeDocumentResponse doc = knowledgeService.updateDocument(id, request);
        return ApiResponse.success("文档信息已更新", doc);
    }

    /**
     * 问答测试
     * POST /api/v1/knowledge/test
     */
    @PostMapping("/test")
    public ApiResponse<KnowledgeTestResponse> testQa(@RequestBody KnowledgeTestRequest request) {
        KnowledgeTestResponse result = knowledgeService.testQa(request);
        return ApiResponse.success(result);
    }
}
