package com.greenhouse.ai.baidu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.ai.DiseaseRecognitionProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 百度 AI 图像识别实现
 * <p>
 * 调用百度 AI 开放平台的植物识别 API。
 * 需要先通过 API Key + Secret Key 获取 Access Token。
 * </p>
 *
 * <h3>API 文档</h3>
 * <ul>
 *   <li>获取 Token：POST https://aip.baidubce.com/oauth/2.0/token</li>
 *   <li>植物识别：POST https://aip.baidubce.com/rest/2.0/image-classify/v1/plant</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.image.provider", havingValue = "baidu", matchIfMissing = true)
public class BaiduRecognitionProvider implements DiseaseRecognitionProvider {

    private final String apiKey;
    private final String secretKey;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    /** 缓存的 Access Token */
    private volatile String cachedToken;
    private volatile long tokenExpireTime;

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String PLANT_RECOGNIZE_URL =
            "https://aip.baidubce.com/rest/2.0/image-classify/v1/plant";

    public BaiduRecognitionProvider(
            @Value("${baidu.ai.api-key}") String apiKey,
            @Value("${baidu.ai.secret-key}") String secretKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public RecognitionResult recognize(byte[] imageBytes) throws Exception {
        String token = getAccessToken();
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 构建请求
        RequestBody body = new FormBody.Builder()
                .add("image", base64Image)
                .add("baike_num", "1")  // 返回百科信息
                .build();

        Request request = new Request.Builder()
                .url(PLANT_RECOGNIZE_URL + "?access_token=" + token)
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("百度 AI 请求失败: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return parseResponse(responseBody);
        }
    }

    /**
     * 获取百度 AI Access Token（带缓存）
     */
    private synchronized String getAccessToken() throws IOException {
        // 检查缓存
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedToken;
        }

        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", apiKey)
                .add("client_secret", secretKey)
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取百度 AI Token 失败: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("error")) {
                throw new IOException("百度 AI Token 错误: " + root.get("error_description").asText());
            }

            cachedToken = root.get("access_token").asText();
            // Token 有效期（秒）减去 60 秒缓冲
            long expiresIn = root.get("expires_in").asLong() - 60;
            tokenExpireTime = System.currentTimeMillis() + expiresIn * 1000;

            log.info("百度 AI Token 已获取，有效期 {} 秒", expiresIn);
            return cachedToken;
        }
    }

    /**
     * 解析百度植物识别 API 响应
     */
    private RecognitionResult parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // 检查错误
        if (root.has("error_code")) {
            int errorCode = root.get("error_code").asInt();
            String errorMsg = root.has("error_msg") ? root.get("error_msg").asText() : "未知错误";
            throw new IOException("百度 AI 识别错误: [" + errorCode + "] " + errorMsg);
        }

        JsonNode results = root.get("result");
        if (results == null || !results.isArray() || results.isEmpty()) {
            return new RecognitionResult("未识别到病虫害", 0.0,
                    "请尝试拍摄更清晰的照片，确保病虫害区域在画面中央", "百度AI");
        }

        // 取置信度最高的结果
        JsonNode best = results.get(0);
        String name = best.get("name").asText();
        double score = best.get("score").asDouble();

        // 提取百科描述作为防治方案
        StringBuilder treatment = new StringBuilder();
        JsonNode baikeInfo = best.get("baike_info");
        if (baikeInfo != null && baikeInfo.has("description")) {
            treatment.append(baikeInfo.get("description").asText());
        }
        if (treatment.isEmpty()) {
            treatment.append("建议咨询农业专家获取详细防治方案");
        }

        log.info("百度 AI 识别结果: name={}, confidence={}", name, score);

        return new RecognitionResult(name, score, treatment.toString(), "百度AI");
    }
}
