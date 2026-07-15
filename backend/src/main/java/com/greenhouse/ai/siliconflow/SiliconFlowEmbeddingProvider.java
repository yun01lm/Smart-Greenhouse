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
        return callApi(texts);
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

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("SiliconFlow API 请求失败: HTTP {}", response.code());
                throw new BusinessException(ErrorCode.AI_EMBEDDING_FAILED);
            }

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
        }
    }
}
