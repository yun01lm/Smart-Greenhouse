package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.PageResult;
import com.greenhouse.module.admin.dto.DialectCorpusResponse;
import com.greenhouse.module.admin.service.AdminCorpusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理员方言语料管理 API
 * <p>
 * 路径前缀：/api/v1/admin/corpus，仅 ADMIN 角色可访问。
 * 支持语料上传（音频+标注）、列表查询（分页+筛选）、删除。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/corpus")
@RequiredArgsConstructor
public class AdminCorpusController {

    private final AdminCorpusService corpusService;

    /**
     * 语料列表（分页 + 筛选）
     * GET /api/v1/admin/corpus?dialect=&keyword=&page=0&size=20
     */
    @GetMapping
    public ApiResponse<PageResult<DialectCorpusResponse>> list(
            @RequestParam(required = false) String dialect,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<DialectCorpusResponse> result = corpusService.list(dialect, keyword, page, size);
        return ApiResponse.success(PageResult.of(
                result.getContent(), result.getTotalElements(), page, size));
    }

    /**
     * 获取所有方言类型（用于筛选下拉）
     * GET /api/v1/admin/corpus/dialects
     */
    @GetMapping("/dialects")
    public ApiResponse<List<String>> getDialects() {
        return ApiResponse.success(corpusService.getDialects());
    }

    /**
     * 上传语料
     * POST /api/v1/admin/corpus
     */
    @PostMapping
    public ApiResponse<DialectCorpusResponse> upload(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("dialect") String dialect,
            @RequestParam(value = "annotationText", required = false) String annotationText,
            @RequestParam(value = "dialectText", required = false) String dialectText,
            @RequestParam(value = "source", defaultValue = "MANUAL") String source,
            @RequestParam(value = "remark", required = false) String remark) {

        log.info("[ADMIN] 上传方言语料: dialect={}, filename={}", dialect,
                audio.getOriginalFilename());
        return ApiResponse.success("语料上传成功",
                corpusService.upload(audio, dialect, annotationText, dialectText, source, remark));
    }

    /**
     * 删除语料
     * DELETE /api/v1/admin/corpus/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        corpusService.delete(id);
        return ApiResponse.success("语料已删除", null);
    }
}
