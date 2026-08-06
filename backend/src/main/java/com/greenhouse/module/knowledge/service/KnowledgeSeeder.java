package com.greenhouse.module.knowledge.service;

import com.greenhouse.entity.KnowledgeDocument;
import com.greenhouse.repository.KnowledgeDocumentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 知识库种子数据初始化
 * <p>
 * 应用启动后检查知识库是否为空，如为空则自动创建示例知识文档。
 * 种子文档源文件置于 classpath 的 knowledge-seed/ 目录（随仓库分发），
 * 初始化时将内容写入上传目录，保证克隆后可复现，不依赖手工放置文件。
 * 初始化过程：
 * 1. 将种子内容写入 uploads/knowledge/ 目录
 * 2. 创建 MySQL 元数据记录
 * 3. 调用 indexDocument 进行文本切片和向量化
 * 4. 将向量写入 ChromaDB
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSeeder {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeService knowledgeService;

    /** 种子资源目录（classpath） */
    private static final String SEED_RESOURCE_DIR = "knowledge-seed";
    /** 上传目录下的知识库子目录 */
    private static final String KNOWLEDGE_SUB_DIR = "knowledge";

    @Value("${file.upload-dir:./uploads}")
    private String uploadDirPath;

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
        // 1. 从 classpath 读取种子内容
        Resource resource = new ClassPathResource(SEED_RESOURCE_DIR + "/" + seed.filename);
        if (!resource.exists()) {
            log.warn("种子文档资源不存在: {}", resource.getDescription());
            return;
        }
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        // 2. 写入上传目录（文件已存在则复用）
        Path uploadRoot = Paths.get(uploadDirPath).toAbsolutePath();
        Path targetDir = uploadRoot.resolve(KNOWLEDGE_SUB_DIR);
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(seed.filename);
        if (!Files.exists(targetFile)) {
            Files.write(targetFile, content.getBytes(StandardCharsets.UTF_8));
        }
        long fileSize = Files.size(targetFile);

        // 3. 创建 MySQL 记录（ID 走统一分配器：优先复用回收池，池空取计数器）
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .id(knowledgeService.allocateDocumentId().id())
                .title(seed.title)
                .category(seed.category)
                .filePath(KNOWLEDGE_SUB_DIR + "/" + seed.filename)
                .fileType("md")
                .fileSize(fileSize)
                .chunkCount(0)
                .vectorIndexed(false)
                .build();
        doc = documentRepository.save(doc);
        // 生成默认文档编号（与上传逻辑一致）
        if (doc.getDocNo() == null || doc.getDocNo().isBlank()) {
            doc.setDocNo("DOC-" + String.format("%04d", doc.getId()));
            doc = documentRepository.save(doc);
        }
        log.info("知识文档元数据已创建: id={}, title={}, category={}", doc.getId(), seed.title, seed.category);

        // 4. 向量化索引
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