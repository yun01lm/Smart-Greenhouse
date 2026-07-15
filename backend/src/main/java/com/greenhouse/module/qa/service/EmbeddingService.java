package com.greenhouse.module.qa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenhouse.ai.EmbeddingProvider;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 文本向量化服务
 * <p>
 * 调用 SiliconFlow bge-m3 API 将文本转为 1024 维向量。
 * bge-m3 是中英文双语 Embedding 模型，支持 8192 token 上下文。
 * </p>
 *
 * <h3>API 端点</h3>
 * POST https://api.siliconflow.cn/v1/embeddings
 *
 * <h3>参考</h3>
 * <ul>
 *   <li>SiliconFlow 文档：https://docs.siliconflow.cn/api-reference/embeddings/create-embeddings</li>
 *   <li>bge-m3 模型：BAAI/bge-m3</li>
 * </ul>
 */
@Slf4j
@Service
public class EmbeddingService {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final EmbeddingProvider embeddingProvider;  // Mock/真实 切换

    public EmbeddingService(
            @Value("${siliconflow.api-key}") String apiKey,
            @Value("${siliconflow.base-url}") String baseUrl,
            @Value("${siliconflow.embedding-model}") String model,
            ObjectMapper objectMapper,
            EmbeddingProvider embeddingProvider) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        this.embeddingProvider = embeddingProvider;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 将文本向量化
     *
     * @param text 待向量化的文本
     * @return 1024 维向量
     */
    public List<Double> embed(String text) {
        try {
            // 优先使用 EmbeddingProvider（支持 Mock 模式）
            float[] vec = embeddingProvider.embed(text);
            List<Double> result = new java.util.ArrayList<>();
            for (float v : vec) result.add((double) v);
            log.debug("向量化完成(Provider): engine={}, dimensions={}, text_length={}",
                    embeddingProvider.getEngineName(), result.size(), text.length());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("向量化异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
        }
    }

    /**
     * 批量向量化（用于知识库索引构建，Phase 4 使用）
     */
    public List<List<Double>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 优先使用 EmbeddingProvider（支持 Mock 模式）
            List<float[]> vecs = embeddingProvider.embedBatch(texts);
            List<List<Double>> results = new java.util.ArrayList<>();
            for (float[] vec : vecs) {
                List<Double> list = new java.util.ArrayList<>();
                for (float v : vec) list.add((double) v);
                results.add(list);
            }
            return results;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量向量化异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
        }
    }
}
