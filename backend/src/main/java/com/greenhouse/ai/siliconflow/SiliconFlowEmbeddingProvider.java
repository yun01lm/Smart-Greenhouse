package com.greenhouse.ai.siliconflow;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * SiliconFlow Embedding Provider
 * <p>
 * 调用 SiliconFlow bge-m3 API 进行文本向量化。
 * 当 ai.embedding.provider=siliconflow 时激活。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.embedding.provider", havingValue = "siliconflow")
public class SiliconFlowEmbeddingProvider implements EmbeddingProvider {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int dimension;

    /** 全局串行信号量：限制 Embedding API 并发，避免触发 429 限流 */
    private static final Semaphore API_LOCK = new Semaphore(1);

    public SiliconFlowEmbeddingProvider(
            @Value("${siliconflow.api-key}") String apiKey,
            @Value("${siliconflow.base-url}") String baseUrl,
            @Value("${siliconflow.embedding-model}") String model,
            @Value("${siliconflow.embedding-dimension:1024}") int dimension,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.dimension = dimension;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public float[] embed(String text) throws Exception {
        List<float[]> results = callApi(List.of(text));
        return results.isEmpty() ? new float[dimension] : results.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) throws Exception {
        // 分批调用（每批 32 条）：避免单次请求体过大，同时批次间留间隔防 429 限流
        List<float[]> results = new ArrayList<>();
        int batchSize = 32;
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            results.addAll(callApi(texts.subList(i, end)));
            if (end < texts.size()) {
                Thread.sleep(2000L);
            }
        }
        return results;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getEngineName() {
        return "siliconflow";
    }

    private List<float[]> callApi(List<String> texts) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        ArrayNode input = objectMapper.createArrayNode();
        for (String text : texts) input.add(text);
        requestBody.set("input", input);

        Request request = new Request.Builder()
                .url(baseUrl + "/embeddings")
                .post(RequestBody.create(
                        objectMapper.writeValueAsString(requestBody),
                        MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        // SiliconFlow 免费额度为 TPM（token/分钟）限流：429 时需等待约 60s 窗口后重试
        int maxRetry = 5;
        for (int attempt = 0; ; attempt++) {
            int statusCode = -1;
            String errorBody = "";
            API_LOCK.acquire();
            try {
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return parseEmbeddings(response);
                    }
                    statusCode = response.code();
                    errorBody = response.body() != null ? response.body().string() : "";
                }
            } finally {
                API_LOCK.release();
            }
            if (statusCode == 429 && attempt < maxRetry) {
                long waitMs = 60_000L + attempt * 30_000L; // 60s/90s/120s/150s/180s
                log.warn("SiliconFlow TPM 限流(429): {}，{}ms 后重试 ({}/{})",
                        errorBody, waitMs, attempt + 1, maxRetry);
                Thread.sleep(waitMs);
                continue;
            }
            log.error("SiliconFlow API 请求失败: HTTP {} body={}", statusCode, errorBody);
            throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
        }
    }

    /** 解析 embeddings 响应（data[].embedding） */
    private List<float[]> parseEmbeddings(Response response) throws Exception {
        try {
            String body = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
            }

            List<float[]> result = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embedding = item.get("embedding");
                float[] vec = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vec[i] = (float) embedding.get(i).asDouble();
                }
                result.add(vec);
            }
            return result;
        } finally {
            response.close();
        }
    }
}
