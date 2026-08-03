package com.greenhouse.module.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.KnowledgeDocument;
import com.greenhouse.module.knowledge.dto.KnowledgeDocumentResponse;
import com.greenhouse.module.knowledge.dto.KnowledgeTestRequest;
import com.greenhouse.module.knowledge.dto.KnowledgeTestResponse;
import com.greenhouse.module.qa.service.ChromaRetrievalService;
import com.greenhouse.module.qa.service.ChromaInitializer;
import com.greenhouse.module.qa.service.EmbeddingService;
import com.greenhouse.module.qa.service.RagQaService;
import com.greenhouse.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    @Autowired
    public KnowledgeService(
            KnowledgeDocumentRepository documentRepository,
            EmbeddingService embeddingService,
            RagQaService ragQaService,
            ChromaInitializer chromaInitializer,
            ObjectMapper objectMapper,
            @Value("${chroma.base-url:http://localhost:8000}") String chromaUrl,
            @Value("${chroma.collection:greenhouse_knowledge}") String collectionName,
            @Value("${file.upload-dir:./uploads}") String uploadDirPath) {
        this.documentRepository = documentRepository;
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

        // 3. 创建 MySQL 记录（初始状态：未向量化）
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .title(docTitle)
                .category(category)
                .filePath(filePath)
                .fileType(extension.replace(".", ""))
                .fileSize(file.getSize())
                .chunkCount(0)
                .vectorIndexed(false)
                .build();
        doc = documentRepository.save(doc);

        log.info("知识库文档已上传: id={}, title={}, category={}", doc.getId(), docTitle, category);

        // 4. 异步触发向量化处理（在当前线程中同步执行，确保即时反馈）
        try {
            indexDocument(doc.getId());
            // 重新加载以获取更新后的状态
            doc = documentRepository.findById(doc.getId()).orElse(doc);
        } catch (Exception e) {
            log.error("文档向量化失败: id={}, error={}", doc.getId(), e.getMessage(), e);
            // 向量化失败不阻塞上传响应，文档保持"待处理"状态
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
            // 1. 读取文件内容
            Path filePath = uploadDir.resolve(doc.getFilePath());
            String content = Files.readString(filePath, StandardCharsets.UTF_8);

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
    private void writeToChroma(KnowledgeDocument doc, List<String> chunks, List<List<Double>> embeddings) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();

            // ids: 每个 chunk 的唯一标识
            ArrayNode ids = objectMapper.createArrayNode();
            for (int i = 0; i < chunks.size(); i++) {
                ids.add("doc_" + doc.getId() + "_chunk_" + i);
            }
            requestBody.set("ids", ids);

            // embeddings: 向量数组
            ArrayNode embeddingsArray = objectMapper.createArrayNode();
            for (List<Double> embedding : embeddings) {
                ArrayNode vec = objectMapper.createArrayNode();
                for (Double v : embedding) {
                    vec.add(v);
                }
                embeddingsArray.add(vec);
            }
            requestBody.set("embeddings", embeddingsArray);

            // documents: 文本内容
            ArrayNode documents = objectMapper.createArrayNode();
            for (String chunk : chunks) {
                documents.add(chunk);
            }
            requestBody.set("documents", documents);

            // metadatas: 每个 chunk 的元数据
            ArrayNode metadatas = objectMapper.createArrayNode();
            for (int i = 0; i < chunks.size(); i++) {
                ObjectNode meta = objectMapper.createObjectNode();
                meta.put("doc_id", doc.getId());
                meta.put("title", doc.getTitle());
                meta.put("category", doc.getCategory() != null ? doc.getCategory() : "");
                meta.put("chunk_index", i);
                meta.put("source_file", doc.getFilePath());
                metadatas.add(meta);
            }
            requestBody.set("metadatas", metadatas);

            String collectionPath = chromaInitializer.getCollectionPath();
            if (collectionPath == null) {
                throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                        "ChromaDB collection 未初始化，无法写入向量");
            }
            String url = chromaUrl + collectionPath + "/add";

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
                log.info("Chroma 写入成功: doc_id={}, chunks={}", doc.getId(), chunks.size());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Chroma 写入异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED,
                    "向量写入 Chroma 失败: " + e.getMessage());
        }
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
