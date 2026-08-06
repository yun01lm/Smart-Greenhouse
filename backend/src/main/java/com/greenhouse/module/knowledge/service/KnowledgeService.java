package com.greenhouse.module.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.KnowledgeCategory;
import com.greenhouse.entity.KnowledgeDocument;
import com.greenhouse.module.knowledge.dto.KnowledgeDocumentResponse;
import com.greenhouse.module.knowledge.dto.KnowledgeTestRequest;
import com.greenhouse.module.knowledge.dto.KnowledgeTestResponse;
import com.greenhouse.module.knowledge.dto.KnowledgeUpdateRequest;
import com.greenhouse.module.qa.service.ChromaRetrievalService;
import com.greenhouse.module.qa.service.ChromaInitializer;
import com.greenhouse.module.qa.service.EmbeddingService;
import com.greenhouse.module.qa.service.RagQaService;
import com.greenhouse.repository.KnowledgeCategoryRepository;
import com.greenhouse.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 知识库管理服务
 * <p>
 * 负责知识库文档的完整生命周期管理：
 * 文档上传 → 文本提取 → 切片 → 向量化 → Chroma 存储 → MySQL 元数据记录。
 * </p>
 *
 * <h3>文档处理管道</h3>
 * <pre>
 * MultipartFile → 保存文件 → 提取文本 → 切片(500-1000字) → Embedding → Chroma写入 → 更新状态
 * </pre>
 *
 * <h3>支持的文档格式</h3>
 * <ul>
 *   <li>.md / .txt — 直接读取文本</li>
 *   <li>.pdf — 暂不支持（需引入 PDF 解析库），返回友好提示</li>
 *   <li>.docx — 暂不支持，返回友好提示</li>
 * </ul>
 */
@Slf4j
@Service
public class KnowledgeService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeCategoryRepository categoryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final RagQaService ragQaService;
    private final ChromaInitializer chromaInitializer;
    private final ObjectMapper objectMapper;

    private final String chromaUrl;
    private final String collectionName;

    /** 切片大小（字符数） */
    private static final int CHUNK_SIZE = 800;
    /** 相邻切片重叠字符数 */
    private static final int CHUNK_OVERLAP = 200;
    /** 知识库文件最大大小 */
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    /** 允许的文件类型 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".md", ".txt");
    /** 提示不支持但可扩展的类型 */
    private static final Set<String> PLANNED_EXTENSIONS = Set.of(".pdf", ".docx");

    private final Path uploadDir;
    private final OkHttpClient httpClient;

    /** 自引用代理：异步向量化时确保事务/代理生效 */
    @Autowired
    @Lazy
    private KnowledgeService self;

    /** 向量化后台执行线程池（单线程串行，避免并发触发 Embedding API 限流） */
    private final ExecutorService vectorizeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "knowledge-vectorize");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    public KnowledgeService(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeCategoryRepository categoryRepository,
            JdbcTemplate jdbcTemplate,
            EmbeddingService embeddingService,
            RagQaService ragQaService,
            ChromaInitializer chromaInitializer,
            ObjectMapper objectMapper,
            @Value("${chroma.base-url:http://localhost:8000}") String chromaUrl,
            @Value("${chroma.collection:greenhouse_knowledge}") String collectionName,
            @Value("${file.upload-dir:./uploads}") String uploadDirPath) {
        this.documentRepository = documentRepository;
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingService = embeddingService;
        this.ragQaService = ragQaService;
        this.chromaInitializer = chromaInitializer;
        this.objectMapper = objectMapper;
        this.chromaUrl = chromaUrl;
        this.collectionName = collectionName;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", this.uploadDir, e);
        }
    }

    // ===== 文档 CRUD =====

    /**
     * 文档列表（分页 + 分类筛选 + 关键词搜索）
     */
    public Page<KnowledgeDocumentResponse> listDocuments(String category, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size);
        Page<KnowledgeDocument> docPage;

        if (keyword != null && !keyword.isBlank()) {
            if (category != null && !category.isBlank()) {
                // 按分类+关键词搜索
                docPage = documentRepository.findByTitleContainingAndCategory(
                        keyword, category, pageable);
            } else {
                docPage = documentRepository.findByTitleContaining(keyword, pageable);
            }
        } else if (category != null && !category.isBlank()) {
            docPage = documentRepository.findByCategory(category, pageable);
        } else {
            docPage = documentRepository.findAll(pageable);
        }

        return docPage.map(KnowledgeDocumentResponse::fromEntity);
    }

    /**
     * 获取分类列表（去重）
     */
    public List<String> getCategories() {
        return documentRepository.findDistinctCategories();
    }

    /**
     * 更新文档标记信息（编号/标题/分类/简介）
     * <p>
     * 仅更新元数据；已向量化的文档会同步更新 Chroma 中对应切片的
     * 元数据（标题/分类/简介），保证 AI 问答引用来源与列表一致。
     * </p>
     *
     * @param documentId 文档ID（系统主键，不可修改）
     * @param request    待更新的标记信息
     * @return 更新后的文档信息
     */
    @Transactional
    public KnowledgeDocumentResponse updateDocument(Long documentId, KnowledgeUpdateRequest request) {
        KnowledgeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "文档不存在"));

        // 1. 文档编号（唯一校验，排除自身）
        if (request.getDocNo() != null) {
            String docNo = request.getDocNo().trim();
            if (docNo.length() > 64) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "文档编号长度不能超过 64 字符");
            }
            if (docNo.isEmpty()) {
                // 清空时回退为默认编号
                docNo = defaultDocNo(documentId);
            }
            if (documentRepository.existsByDocNoAndIdNot(docNo, documentId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "文档编号已被占用：" + docNo);
            }
            doc.setDocNo(docNo);
        }

        // 2. 标题
        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "文档标题不能为空");
            }
            if (title.length() > 200) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "文档标题长度不能超过 200 字符");
            }
            doc.setTitle(title);
        }

        // 3. 分类
        if (request.getCategory() != null) {
            String category = request.getCategory().trim();
            if (category.length() > 100) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "分类长度不能超过 100 字符");
            }
            doc.setCategory(category.isEmpty() ? null : category);
            ensureCategoryRegistered(category);
        }

        // 4. 简介
        if (request.getDescription() != null) {
            String description = request.getDescription().trim();
            if (description.length() > 2000) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "简介长度不能超过 2000 字符");
            }
            doc.setDescription(description.isEmpty() ? null : description);
        }

        doc = documentRepository.save(doc);

        // 5. 已向量化：同步 Chroma 元数据（失败不阻塞编辑保存）
        if (Boolean.TRUE.equals(doc.getVectorIndexed())
                && doc.getChunkCount() != null && doc.getChunkCount() > 0) {
            try {
                updateChromaMetadata(doc);
            } catch (Exception e) {
                log.warn("Chroma 元数据同步失败（编辑已保存，问答引用可能滞后）: id={}, error={}",
                        doc.getId(), e.getMessage());
            }
        }

        log.info("知识库文档已更新: id={}, docNo={}, title={}, category={}",
                doc.getId(), doc.getDocNo(), doc.getTitle(), doc.getCategory());
        return KnowledgeDocumentResponse.fromEntity(doc);
    }

    /**
     * 生成默认文档编号（如 DOC-0001）
     */
    private String defaultDocNo(Long id) {
        return "DOC-" + String.format("%04d", id);
    }

    /**
     * 分配文档 ID
     * <p>
     * 优先复用回收池（knowledge_document_id_recycle）中最小的已删除 ID；
     * 池为空时从计数器（knowledge_document_id_seq）取下一连续值并自增。
     * 通过 SELECT ... FOR UPDATE 保证并发分配不冲突，须在事务中调用。
     * </p>
     *
     * @return 分配结果（ID + 是否复用回收池）
     */
    @Transactional
    public IdAllocation allocateDocumentId() {
        // 1. 优先复用回收池中最小的 ID
        List<Long> recycled = jdbcTemplate.queryForList(
                "SELECT recycled_id FROM knowledge_document_id_recycle ORDER BY recycled_id LIMIT 1 FOR UPDATE",
                Long.class);
        if (!recycled.isEmpty()) {
            Long id = recycled.get(0);
            jdbcTemplate.update("DELETE FROM knowledge_document_id_recycle WHERE recycled_id = ?", id);
            log.info("文档 ID 复用回收池: id={}", id);
            return new IdAllocation(id, true);
        }

        // 2. 池空：取计数器下一个 ID 并自增
        Long nextId = jdbcTemplate.queryForObject(
                "SELECT next_id FROM knowledge_document_id_seq FOR UPDATE", Long.class);
        if (nextId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文档 ID 计数器未初始化");
        }
        jdbcTemplate.update("UPDATE knowledge_document_id_seq SET next_id = next_id + 1");
        return new IdAllocation(nextId, false);
    }

    /**
     * 分类登记兜底：文档使用了分类表中不存在的分类时自动登记
     */
    private void ensureCategoryRegistered(String category) {
        if (category == null || category.isBlank()) {
            return;
        }
        try {
            if (!categoryRepository.existsByName(category)) {
                categoryRepository.save(KnowledgeCategory.builder().name(category).build());
                log.info("新分类已自动登记: {}", category);
            }
        } catch (Exception e) {
            log.warn("分类自动登记失败（不影响文档保存）: category={}, error={}", category, e.getMessage());
        }
    }

    /**
     * 分类重命名级联：更新文档分类并同步已向量化文档的 Chroma 元数据
     *
     * @param oldName 旧分类名
     * @param newName 新分类名
     */
    @Transactional
    public void renameDocumentCategory(String oldName, String newName) {
        List<KnowledgeDocument> docs = documentRepository.findByCategory(oldName);
        int vectorSynced = 0;
        for (KnowledgeDocument doc : docs) {
            doc.setCategory(newName);
            documentRepository.save(doc);
            if (Boolean.TRUE.equals(doc.getVectorIndexed())
                    && doc.getChunkCount() != null && doc.getChunkCount() > 0) {
                try {
                    updateChromaMetadata(doc);
                    vectorSynced++;
                } catch (Exception e) {
                    log.warn("分类重命名 Chroma 同步失败: doc_id={}, error={}", doc.getId(), e.getMessage());
                }
            }
        }
        log.info("分类重命名级联完成: old={}, new={}, docs={}, vectorSynced={}",
                oldName, newName, docs.size(), vectorSynced);
    }

    /**
     * 文档 ID 分配结果
     *
     * @param id       分配到的文档 ID
     * @param recycled 是否来自回收池（复用已删除 ID）
     */
    public record IdAllocation(Long id, boolean recycled) {}

    /**
     * 上传文档（自动触发处理管道）
     *
     * @param file     文档文件
     * @param title    文档标题（可空，默认用文件名）
     * @param category 文档分类
     * @return 文档元数据
     */
    @Transactional
    public KnowledgeDocumentResponse uploadDocument(MultipartFile file, String title, String category) {
        // 1. 校验
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }

        if (PLANNED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    extension + " 格式暂不支持，请使用 .md 或 .txt 格式。PDF/DOCX 支持将在后续版本中添加。");
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                    "不支持的文件格式，请上传 .md 或 .txt 文件");
        }

        // 2. 保存文件
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID().toString() + extension;
        Path targetDir = uploadDir.resolve("knowledge").resolve(datePath);
        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("知识库文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        String filePath = "knowledge/" + datePath + "/" + filename;
        String docTitle = (title != null && !title.isBlank())
                ? title
                : (originalName != null ? originalName : "未命名文档");

        // 3. 分配文档 ID（优先复用回收池中的已删除 ID，池空则取计数器下一值）
        IdAllocation allocation = allocateDocumentId();
        if (allocation.recycled()) {
            // 复用历史 ID：防御性清理 Chroma 中该 ID 的残留向量，避免新旧文档数据串扰
            deleteFromChroma(allocation.id());
            log.info("复用回收 ID 前已清理潜在残留向量: id={}", allocation.id());
        }

        // 4. 分类自动登记（分类表不存在的分类自动入库，保证分类管理完整）
        ensureCategoryRegistered(category);

        // 5. 创建 MySQL 记录（初始状态：未向量化）
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .id(allocation.id())
                .title(docTitle)
                .category(category)
                .filePath(filePath)
                .fileType(extension.replace(".", ""))
                .fileSize(file.getSize())
                .chunkCount(0)
                .vectorIndexed(false)
                .build();
        doc = documentRepository.save(doc);

        // 生成默认文档编号（DOC-xxxx），用户可在编辑功能中修改
        if (doc.getDocNo() == null || doc.getDocNo().isBlank()) {
            doc.setDocNo(defaultDocNo(doc.getId()));
            doc = documentRepository.save(doc);
        }

        log.info("知识库文档已上传: id={}, title={}, category={}", doc.getId(), docTitle, category);

        // 4. 事务提交后异步触发向量化处理（后台线程池执行，上传立即返回，前端无需等待）
        final Long docId = doc.getId();
        Runnable vectorizeTask = () -> {
            try {
                self.indexDocument(docId);
            } catch (Exception e) {
                log.error("后台向量化失败: id={}, error={}", docId, e.getMessage(), e);
            }
        };
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        vectorizeExecutor.submit(vectorizeTask);
                        log.info("知识库向量化已提交后台执行: id={}", docId);
                    }
                });
            } else {
                vectorizeExecutor.submit(vectorizeTask);
                log.info("知识库向量化已提交后台执行: id={}", docId);
            }
        } catch (Exception e) {
            log.error("向量化任务提交失败: id={}, error={}", docId, e.getMessage(), e);
            // 任务提交失败不阻塞上传响应，文档保持"待向量化"状态，可手动重试
        }

        return KnowledgeDocumentResponse.fromEntity(doc);
    }

    /**
     * 触发向量化索引
     *
     * @param documentId 文档ID
     * @return 更新后的文档信息
     */
    @Transactional
    public KnowledgeDocumentResponse indexDocument(Long documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "文档不存在"));

        log.info("开始向量化处理: id={}, title={}", doc.getId(), doc.getTitle());

        try {
            // 0. 幂等处理：向量化前统一清理该文档旧向量（避免重复写入与历史残留，含 ID 复用场景）
            deleteFromChroma(doc.getId());
            log.info("向量化前已清理旧向量: doc_id={}", doc.getId());

            // 1. 读取文件内容（自动识别 UTF-8 / GBK 编码）
            Path filePath = uploadDir.resolve(doc.getFilePath());
            String content = readFileContent(filePath);

            // 2. 文本切片
            List<String> chunks = splitText(content);

            // 3. 批量向量化
            List<List<Double>> embeddings = embeddingService.embedBatch(chunks);

            // 4. 写入 Chroma
            writeToChroma(doc, chunks, embeddings);

            // 5. 更新 MySQL 状态
            doc.setChunkCount(chunks.size());
            doc.setVectorIndexed(true);
            doc.setIndexedAt(LocalDateTime.now());
            documentRepository.save(doc);

            log.info("向量化完成: id={}, chunks={}", doc.getId(), chunks.size());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("向量化处理异常: id={}, error={}", doc.getId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED, "向量化处理失败: " + e.getMessage());
        }

        return KnowledgeDocumentResponse.fromEntity(doc);
    }

    /**
     * 删除文档（同步清理 Chroma 向量 + MySQL 记录 + 本地文件）
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        KnowledgeDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "文档不存在"));

        // 1. 从 Chroma 删除向量（按 doc_id 过滤删除）
        if (doc.getVectorIndexed()) {
            try {
                deleteFromChroma(doc.getId());
            } catch (Exception e) {
                log.warn("Chroma 向量删除失败（继续删除 MySQL 记录）: id={}, error={}",
                        doc.getId(), e.getMessage());
            }
        }

        // 2. 删除本地文件（filePath 可能为 NULL，如历史损坏数据）
        if (doc.getFilePath() != null) {
            try {
                Path filePath = uploadDir.resolve(doc.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("本地文件删除失败: path={}, error={}", doc.getFilePath(), e.getMessage());
            }
        }

        // 3. 删除 MySQL 记录
        documentRepository.delete(doc);

        // 4. 回收 ID：写入回收池，供后续新增文档优先复用
        try {
            jdbcTemplate.update(
                    "INSERT INTO knowledge_document_id_recycle (recycled_id, created_at) VALUES (?, NOW())",
                    documentId);
            log.info("文档 ID 已回收: id={}", documentId);
        } catch (Exception e) {
            log.warn("文档 ID 回收失败（不影响删除）: id={}, error={}", documentId, e.getMessage());
        }

        log.info("知识库文档已删除: id={}, title={}", doc.getId(), doc.getTitle());
    }

    /**
     * 问答测试
     */
    public KnowledgeTestResponse testQa(KnowledgeTestRequest request) {
        long startTime = System.currentTimeMillis();

        // 调用已有的 RagQaService 生成回答
        RagQaService.AnswerResult result = ragQaService.generateAnswerOnly(request.getQuestion());

        // 构建检索片段信息
        List<KnowledgeTestResponse.RetrievedChunk> chunks = result.sources().stream()
                .map(source -> KnowledgeTestResponse.RetrievedChunk.builder()
                        .documentTitle(source.getTitle())
                        .content(source.getCategory()) // category 字段存的是内容摘要
                        .score(0.0) // RagQaService 暂不返回分数
                        .build())
                .collect(Collectors.toList());

        long responseTime = System.currentTimeMillis() - startTime;

        return KnowledgeTestResponse.builder()
                .question(request.getQuestion())
                .answer(result.answer())
                .retrievedChunks(chunks)
                .responseTime(responseTime)
                .build();
    }

    // ===== 文档处理管道 =====

    /**
     * 文本切片
     * <p>
     * 策略：按段落边界切片，每片 500-1000 字符，相邻切片保留 200 字符重叠。
     * 以空行（连续两个换行）为段落分割点。
     * </p>
     */
    /**
     * 读取知识库文件文本内容
     * <p>
     * 优先按 UTF-8 严格解码；失败（如 Windows 记事本默认保存的 GBK/ANSI 文件）
     * 回退到 GBK 解码，避免 MalformedInputException 导致向量化失败。
     * </p>
     */
    private String readFileContent(Path filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            log.info("UTF-8 解码失败，回退 GBK: file={}, error={}",
                    filePath.getFileName(), e.getMessage());
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    List<String> splitText(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\\n\\s*\\n"); // 以空行为分割

        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            // 如果当前块 + 新段落超过 CHUNK_SIZE，先保存当前块
            if (currentChunk.length() + trimmed.length() > CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());

                // 保留重叠部分（最后 CHUNK_OVERLAP 个字符）
                if (currentChunk.length() > CHUNK_OVERLAP) {
                    String overlap = currentChunk.substring(
                            currentChunk.length() - CHUNK_OVERLAP);
                    currentChunk = new StringBuilder(overlap);
                } else {
                    currentChunk = new StringBuilder();
                }
            }

            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmed);
        }

        // 最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.debug("文本切片完成: total_chars={}, chunks={}", content.length(), chunks.size());
        return chunks;
    }

    /**
     * 将切片和向量写入 Chroma
     * <p>
     * 使用 Chroma REST API：POST /api/v1/collections/{name}/add
     * </p>
     */
    /**
     * 将切片和向量写入 Chroma
     * <p>
     * 使用 Chroma REST API：POST /api/v1/collections/{name}/add
     * 分批写入（每批 200 条），避免大文档单次请求体过大导致失败。
     * </p>
     */
    private void writeToChroma(KnowledgeDocument doc, List<String> chunks, List<List<Double>> embeddings) {
        int batchSize = 200;
        String collectionPath = chromaInitializer.getCollectionPath();
        if (collectionPath == null) {
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                    "ChromaDB collection 未初始化，无法写入向量");
        }
        String url = chromaUrl + collectionPath + "/add";

        int total = chunks.size();
        for (int start = 0; start < total; start += batchSize) {
            int end = Math.min(start + batchSize, total);
            try {
                ObjectNode requestBody = objectMapper.createObjectNode();

                ArrayNode ids = objectMapper.createArrayNode();
                ArrayNode embeddingsArray = objectMapper.createArrayNode();
                ArrayNode documents = objectMapper.createArrayNode();
                ArrayNode metadatas = objectMapper.createArrayNode();

                for (int i = start; i < end; i++) {
                    ids.add("doc_" + doc.getId() + "_chunk_" + i);

                    ArrayNode vec = objectMapper.createArrayNode();
                    for (Double v : embeddings.get(i)) {
                        vec.add(v);
                    }
                    embeddingsArray.add(vec);

                    documents.add(chunks.get(i));

                    ObjectNode meta = objectMapper.createObjectNode();
                    meta.put("doc_id", doc.getId());
                    meta.put("title", doc.getTitle());
                    meta.put("category", doc.getCategory() != null ? doc.getCategory() : "");
                    meta.put("chunk_index", i);
                    meta.put("source_file", doc.getFilePath());
                    metadatas.add(meta);
                }
                requestBody.set("ids", ids);
                requestBody.set("embeddings", embeddingsArray);
                requestBody.set("documents", documents);
                requestBody.set("metadatas", metadatas);

                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(
                                objectMapper.writeValueAsString(requestBody),
                                MediaType.parse("application/json")))
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        log.error("Chroma 写入失败: HTTP {} body={}", response.code(), errorBody);
                        throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                                "向量写入 Chroma 失败: HTTP " + response.code());
                    }
                }
                log.info("Chroma 写入进度: doc_id={}, batch=[{}-{})", doc.getId(), start, end);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("Chroma 写入异常: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                        "向量写入 Chroma 失败: " + e.getMessage());
            }
        }
        log.info("Chroma 写入完成: doc_id={}, chunks={}", doc.getId(), total);
    }

    /**
     * 更新 Chroma 中某文档全部切片的元数据（标题/分类/简介）
     * <p>
     * Chroma v2 /update 接口需按向量 ID 精确更新（不支持 where 过滤），
     * 因此按 doc_{id}_chunk_{0..chunkCount-1} 拼接全部向量 ID 一次性提交。
     * </p>
     */
    private void updateChromaMetadata(KnowledgeDocument doc) throws Exception {
        String collectionPath = chromaInitializer.getCollectionPath();
        if (collectionPath == null) {
            log.warn("ChromaDB collection 未初始化，跳过元数据同步: doc_id={}", doc.getId());
            return;
        }
        String url = chromaUrl + collectionPath + "/update";

        int total = doc.getChunkCount();
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode ids = objectMapper.createArrayNode();
        ArrayNode metadatas = objectMapper.createArrayNode();
        for (int i = 0; i < total; i++) {
            ids.add("doc_" + doc.getId() + "_chunk_" + i);
            ObjectNode meta = objectMapper.createObjectNode();
            meta.put("doc_id", doc.getId());
            meta.put("title", doc.getTitle());
            meta.put("category", doc.getCategory() != null ? doc.getCategory() : "");
            meta.put("description", doc.getDescription() != null ? doc.getDescription() : "");
            meta.put("chunk_index", i);
            meta.put("source_file", doc.getFilePath());
            metadatas.add(meta);
        }
        body.set("ids", ids);
        body.set("metadatas", metadatas);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(
                        objectMapper.writeValueAsString(body),
                        MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                        "Chroma 元数据更新失败: HTTP " + response.code() + " body=" + errorBody);
            }
        }
        log.info("Chroma 元数据已同步: doc_id={}, chunks={}", doc.getId(), total);
    }

    /**
     * 从 Chroma 删除文档的所有向量
     * <p>
     * 使用 Chroma REST API：POST /api/v1/collections/{name}/delete
     * 通过 metadata 过滤：{"doc_id": documentId}
     * </p>
     */
    private void deleteFromChroma(Long documentId) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();

            // 按 metadata 过滤删除
            ObjectNode whereFilter = objectMapper.createObjectNode();
            whereFilter.put("doc_id", documentId);
            requestBody.set("where", whereFilter);

            String collectionPath = chromaInitializer.getCollectionPath();
            if (collectionPath == null) {
                log.warn("ChromaDB collection 未初始化，跳过删除");
                return;
            }
            String url = chromaUrl + collectionPath + "/delete";

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")))
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Chroma 删除失败: HTTP {} (向量可能已被清理)",
                            response.code());
                } else {
                    log.info("Chroma 向量已删除: doc_id={}", documentId);
                }
            }
        } catch (Exception e) {
            log.warn("Chroma 删除异常: doc_id={}, error={}", documentId, e.getMessage());
            // 不抛异常，允许 MySQL 记录正常删除
        }
    }
}
