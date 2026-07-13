package com.greenhouse.module.qa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    public EmbeddingService(
            @Value("${siliconflow.api-key}") String apiKey,
            @Value("${siliconflow.base-url}") String baseUrl,
            @Value("${siliconflow.embedding-model}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;
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
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("input", text);
            requestBody.put("encoding_format", "float");

            Request request = new Request.Builder()
                    .url(baseUrl + "/embeddings")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Embedding 请求失败: HTTP {} body={}", response.code(), errorBody);
                    throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseBody);

                // 解析 data[0].embedding
                JsonNode data = root.get("data");
                if (data == null || !data.isArray() || data.isEmpty()) {
                    throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
                }

                JsonNode embedding = data.get(0).get("embedding");
                if (embedding == null || !embedding.isArray()) {
                    throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
                }

                List<Double> result = new java.util.ArrayList<>();
                for (JsonNode value : embedding) {
                    result.add(value.asDouble());
                }

                log.debug("向量化完成: model={}, dimensions={}, text_length={}",
                        model, result.size(), text.length());
                return result;
            }
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
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("encoding_format", "float");

            ArrayNode inputArray = objectMapper.createArrayNode();
            for (String text : texts) {
                inputArray.add(text);
            }
            requestBody.set("input", inputArray);

            Request request = new Request.Builder()
                    .url(baseUrl + "/embeddings")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode data = root.get("data");

                List<List<Double>> results = new java.util.ArrayList<>();
                if (data != null && data.isArray()) {
                    for (JsonNode item : data) {
                        JsonNode embedding = item.get("embedding");
                        List<Double> vec = new java.util.ArrayList<>();
                        if (embedding != null && embedding.isArray()) {
                            for (JsonNode value : embedding) {
                                vec.add(value.asDouble());
                            }
                        }
                        results.add(vec);
                    }
                }
                return results;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量向量化异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
        }
    }
}
