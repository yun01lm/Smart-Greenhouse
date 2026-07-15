package com.greenhouse.ai.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenhouse.ai.LlmProvider;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * DeepSeek LLM Provider
 * <p>
 * 通过 OpenAI 兼容 API 调用 DeepSeek 大模型。
 * 当 ai.llm.provider=deepseek 时激活。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.llm.provider", havingValue = "deepseek")
public class DeepSeekLlmProvider implements LlmProvider {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekLlmProvider(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String generate(String systemPrompt, String userMessage,
                           double temperature, int maxTokens) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.set("messages", messages);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(RequestBody.create(
                        objectMapper.writeValueAsString(requestBody),
                        MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
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
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_LLM_FAILED);
            }
            JsonNode message = choices.get(0).get("message");
            if (message == null || !message.has("content")) {
                throw new BusinessException(ErrorCode.AI_LLM_FAILED);
            }
            return message.get("content").asText();
        }
    }

    @Override
    public String getEngineName() {
        return "deepseek";
    }
}
