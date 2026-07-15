package com.greenhouse.module.qa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.QaRecord;
import com.greenhouse.module.qa.dto.QaResponse;
import com.greenhouse.repository.QaRecordRepository;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RAG 问答核心服务
 * <p>
 * 完整链路：用户问题 → 向量化(EmbeddingService) → Chroma检索(ChromaRetrievalService)
 * → Prompt组装 → DeepSeek生成 → 保存记录 → 返回结果。
 * </p>
 *
 * <h3>配置化说明</h3>
 * <p>
 * Prompt、temperature、max_tokens、top-k 均从 application.yml 读取，
 * 支持运行时调整和论文 Prompt 工程对比实验。
 * </p>
 *
 * <h3>降级策略</h3>
 * <ul>
 *   <li>Chroma 不可用时：使用 DeepSeek 通用知识回答，标注"基于通用知识"</li>
 *   <li>DeepSeek 不可用时：返回错误提示</li>
 * </ul>
 */
@Slf4j
@Service
public class RagQaService {

    private final EmbeddingService embeddingService;
    private final ChromaRetrievalService retrievalService;
    private final QaRecordRepository recordRepository;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    private final String deepseekApiKey;
    private final String deepseekBaseUrl;
    private final String deepseekModel;

    /** RAG 系统提示词（从 application.yml 读取，支持运行时调整） */
    private final String systemPrompt;

    /** RAG 检索 Top-K（从 application.yml 读取，默认 5） */
    private final int topK;

    /** LLM 温度参数（从 application.yml 读取，默认 0.7） */
    private final double temperature;

    /** LLM 最大生成 token 数（从 application.yml 读取，默认 2000） */
    private final int maxTokens;

    public RagQaService(
            EmbeddingService embeddingService,
            ChromaRetrievalService retrievalService,
            QaRecordRepository recordRepository,
            ObjectMapper objectMapper,
            @Value("${deepseek.api-key}") String deepseekApiKey,
            @Value("${deepseek.base-url}") String deepseekBaseUrl,
            @Value("${deepseek.model}") String deepseekModel,
            @Value("${greenhouse.ai.rag.system-prompt}") String systemPrompt,
            @Value("${greenhouse.ai.rag.top-k:5}") int topK,
            @Value("${greenhouse.ai.rag.temperature:0.7}") double temperature,
            @Value("${greenhouse.ai.rag.max-tokens:2000}") int maxTokens) {
        this.embeddingService = embeddingService;
        this.retrievalService = retrievalService;
        this.recordRepository = recordRepository;
        this.objectMapper = objectMapper;
        this.deepseekApiKey = deepseekApiKey;
        this.deepseekBaseUrl = deepseekBaseUrl;
        this.deepseekModel = deepseekModel;
        this.systemPrompt = systemPrompt;
        this.topK = topK;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)  // LLM 生成可能较慢
                .build();
    }

    /**
     * 文字问答
     *
     * @param userId       用户ID
     * @param question     问题内容
     * @param greenhouseId 大棚ID（可空）
     * @return 问答结果
     */
    @Transactional
    public QaResponse askText(Long userId, String question, Long greenhouseId) {
        // 1. RAG 检索
        RagContext ragContext = retrieveContext(question);

        // 2. 调用 DeepSeek 生成回答
        String answer = generateAnswer(question, ragContext);

        // 3. 构建引用来源 JSON
        String sourcesJson = buildSourcesJson(ragContext.sources);

        // 4. 保存问答记录
        QaRecord record = QaRecord.builder()
                .userId(userId)
                .question(question)
                .answer(answer)
                .inputType(QaRecord.InputType.TEXT)
                .sources(sourcesJson)
                .createdAt(LocalDateTime.now())
                .build();
        record = recordRepository.save(record);

        log.info("文字问答完成: id={}, question_length={}, answer_length={}, sources={}",
                record.getId(), question.length(), answer.length(), ragContext.sources.size());

        return QaResponse.fromEntity(record);
    }

    /**
     * 查询问答历史
     */
    public List<QaResponse> getHistory(Long userId, int page, int size) {
        Page<QaRecord> records = recordRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return records.getContent().stream()
                .map(QaResponse::fromEntity)
                .toList();
    }

    // ===== 私有方法 =====

    /**
     * RAG 检索上下文
     */
    private RagContext retrieveContext(String question) {
        List<QaResponse.SourceInfo> sources = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        try {
            // 1. 向量化问题
            List<Double> queryVector = embeddingService.embed(question);

            // 2. Chroma 检索
            List<ChromaRetrievalService.RetrievalResult> results = retrievalService.query(queryVector, topK);

            if (results.isEmpty()) {
                contextBuilder.append("（知识库中暂无相关内容，请基于通用农业知识回答）");
                return new RagContext(contextBuilder.toString(), sources, false);
            }

            // 3. 组装上下文
            for (int i = 0; i < results.size(); i++) {
                ChromaRetrievalService.RetrievalResult result = results.get(i);
                contextBuilder.append(String.format("【参考资料%d】%s\n", i + 1, result.content()));

                sources.add(QaResponse.SourceInfo.builder()
                        .title(result.title())
                        .category(result.category())
                        .build());
            }

            return new RagContext(contextBuilder.toString(), sources, true);
        } catch (Exception e) {
            log.error("RAG 检索异常: {}", e.getMessage(), e);
            // 检索失败不影响回答，降级为纯 LLM
            contextBuilder.append("（知识库检索暂时不可用，请基于通用农业知识回答）");
            return new RagContext(contextBuilder.toString(), sources, false);
        }
    }

    /**
     * 调用 DeepSeek 生成回答
     */
    private String generateAnswer(String question, RagContext ragContext) {
        try {
            String prompt = String.format(systemPrompt, ragContext.context);

            // 构建 OpenAI 兼容格式的请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", deepseekModel);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            ArrayNode messages = objectMapper.createArrayNode();

            // system message
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", prompt);
            messages.add(systemMsg);

            // user message
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", question);
            messages.add(userMsg);

            requestBody.set("messages", messages);

            Request request = new Request.Builder()
                    .url(deepseekBaseUrl + "/chat/completions")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + deepseekApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("DeepSeek API 请求失败: HTTP {} body={}", response.code(), errorBody);
                    throw new BusinessException(ErrorCode.AI_LLM_FAILED);
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseBody);

                // 解析 OpenAI 格式: choices[0].message.content
                JsonNode choices = root.get("choices");
                if (choices == null || !choices.isArray() || choices.isEmpty()) {
                    throw new BusinessException(ErrorCode.AI_LLM_FAILED);
                }

                JsonNode message = choices.get(0).get("message");
                if (message == null || !message.has("content")) {
                    throw new BusinessException(ErrorCode.AI_LLM_FAILED);
                }

                String answer = message.get("content").asText();

                // 如果知识库无内容，追加标注
                if (!ragContext.hasKnowledge) {
                    answer += "\n\n---\n*注：以上回答基于通用农业知识，知识库中暂无相关内容。*";
                }

                return answer;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek 调用异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_LLM_FAILED);
        }
    }

    /**
     * 构建引用来源 JSON
     */
    private String buildSourcesJson(List<QaResponse.SourceInfo> sources) {
        try {
            ArrayNode array = objectMapper.createArrayNode();
            for (QaResponse.SourceInfo source : sources) {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("title", source.getTitle());
                node.put("category", source.getCategory());
                array.add(node);
            }
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * 仅生成回答（不保存记录）
     * <p>
     * 供 VoiceQaService 和 KnowledgeService 调用，避免重复保存记录。
     * </p>
     *
     * @param question 问题内容
     * @return RAG 生成结果（answer + sources）
     */
    public AnswerResult generateAnswerOnly(String question) {
        RagContext ragContext = retrieveContext(question);
        String answer = generateAnswer(question, ragContext);
        return new AnswerResult(answer, ragContext.sources, ragContext.hasKnowledge);
    }

    /**
     * RAG 生成结果（不包含数据库记录）
     */
    public record AnswerResult(
            String answer,
            List<QaResponse.SourceInfo> sources,
            boolean hasKnowledge
    ) {}

    /**
     * RAG 检索上下文内部类
     */
    private record RagContext(
            String context,
            List<QaResponse.SourceInfo> sources,
            boolean hasKnowledge
    ) {}
}
