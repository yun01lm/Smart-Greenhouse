package com.greenhouse.module.knowledge.service;

import com.greenhouse.entity.KnowledgeDocument;
import com.greenhouse.repository.KnowledgeDocumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库种子数据初始化
 * <p>
 * 应用启动后检查知识库是否为空，如为空则自动创建示例知识文档。
 * 文件已预置在 uploads/knowledge/ 目录下，初始化过程：
 * 1. 创建 MySQL 元数据记录
 * 2. 调用 indexDocument 进行文本切片和向量化
 * 3. 将向量写入 ChromaDB
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSeeder {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeService knowledgeService;

    private static final String KNOWLEDGE_DIR = "./uploads/knowledge";

    /**
     * 种子文档定义
     */
    private record SeedDoc(String filename, String title, String category) {}

    private static final List<SeedDoc> SEED_DOCS = List.of(
            new SeedDoc("番茄种植技术指南.md", "番茄种植技术指南", "栽培技术"),
            new SeedDoc("常见病虫害防治手册.md", "常见病虫害防治手册", "病虫害防治")
    );

    @PostConstruct
    public void seed() {
        try {
            long count = documentRepository.count();
            if (count > 0) {
                log.info("知识库已有 {} 个文档，跳过种子数据初始化", count);
                return;
            }

            log.info("知识库为空，开始初始化种子文档...");
            int successCount = 0;

            for (SeedDoc seed : SEED_DOCS) {
                try {
                    seedDocument(seed);
                    successCount++;
                } catch (Exception e) {
                    log.error("种子文档初始化失败: {}, error={}", seed.filename, e.getMessage(), e);
                }
            }

            log.info("知识库种子数据初始化完成: {}/{} 成功", successCount, SEED_DOCS.size());
        } catch (Exception e) {
            log.error("知识库种子数据初始化异常: {}", e.getMessage(), e);
        }
    }

    private void seedDocument(SeedDoc seed) throws Exception {
        Path filePath = Paths.get(KNOWLEDGE_DIR, seed.filename);
        if (!Files.exists(filePath)) {
            log.warn("种子文档文件不存在: {}", filePath.toAbsolutePath());
            return;
        }

        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        long fileSize = Files.size(filePath);

        // 1. 创建 MySQL 记录
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .title(seed.title)
                .category(seed.category)
                .filePath("knowledge/" + seed.filename)
                .fileType("md")
                .fileSize(fileSize)
                .chunkCount(0)
                .vectorIndexed(false)
                .build();
        doc = documentRepository.save(doc);
        log.info("知识文档元数据已创建: id={}, title={}", doc.getId(), seed.title);

        // 2. 向量化索引
        try {
            knowledgeService.indexDocument(doc.getId());
            log.info("知识文档向量化完成: id={}, title={}", doc.getId(), seed.title);
        } catch (Exception e) {
            log.error("知识文档向量化失败: id={}, title={}, error={}",
                    doc.getId(), seed.title, e.getMessage(), e);
            // 不阻塞启动，文档保持"待处理"状态
        }
    }
}
