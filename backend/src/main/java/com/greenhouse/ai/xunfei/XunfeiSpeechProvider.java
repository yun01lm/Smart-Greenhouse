package com.greenhouse.ai.xunfei;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.ai.SpeechRecognitionProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞语音识别实现
 * <p>
 * 调用讯飞 WebAPI 进行语音转文字，支持河北方言识别。
 * 使用 HMAC-SHA256 签名鉴权方式。
 * </p>
 *
 * <h3>API 文档</h3>
 * <ul>
 *   <li>WebAPI 文档：https://www.xfyun.cn/doc/asr/voicedictation/API.html</li>
 *   <li>方言支持：hebei（河北话）</li>
 * </ul>
 *
 * <h3>配置项</h3>
 * <ul>
 *   <li>{@code xunfei.app-id} — 讯飞应用 ID</li>
 *   <li>{@code xunfei.api-key} — 讯飞 API Key</li>
 *   <li>{@code xunfei.api-secret} — 讯飞 API Secret（用于签名）</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.voice.provider", havingValue = "xunfei", matchIfMissing = true)
public class XunfeiSpeechProvider implements SpeechRecognitionProvider {

    private final String appId;
    private final String apiKey;
    private final String apiSecret;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    /** 讯飞语音听写 WebAPI 地址 */
    private static final String ASR_URL = "https://ws-api.xfyun.cn/v2/aisp";

    /** 支持的方言 */
    private static final List<String> SUPPORTED_DIALECTS = List.of("mandarin", "hebei");

    public XunfeiSpeechProvider(
            @Value("${xunfei.app-id}") String appId,
            @Value("${xunfei.api-key}") String apiKey,
            @Value("${xunfei.api-secret}") String apiSecret,
            ObjectMapper objectMapper) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public SpeechRecognitionResult recognize(byte[] audioData) throws Exception {
        // 1. 构建请求 URL（带签名参数）
        String requestUrl = buildAuthUrl();

        // 2. Base64 编码音频
        String base64Audio = Base64.getEncoder().encodeToString(audioData);

        // 3. 构建请求体
        String requestBody = buildRequestBody(base64Audio);

        // 4. 发送请求
        Request request = new Request.Builder()
                .url(requestUrl)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("讯飞 ASR 请求失败: HTTP " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            return parseResponse(responseBody);
        }
    }

    @Override
    public String getEngineName() {
        return "xunfei";
    }

    @Override
    public List<String> getSupportedDialects() {
        return SUPPORTED_DIALECTS;
    }

    // ===== 私有方法 =====

    /**
     * 构建带 HMAC-SHA256 签名的请求 URL
     * <p>
     * 讯飞鉴权方式：对 host + date + request-line 做 HMAC-SHA256 签名，
     * 结果编码为 authorization header 参数。
     * </p>
     */
    private String buildAuthUrl() throws Exception {
        URL url = new URL(ASR_URL);
        String host = url.getHost();

        // RFC 1123 格式时间
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        // 签名字符串: host: host\ndate: date\nGET /v2/aisp HTTP/1.1
        String signatureOrigin = "host: " + host + "\ndate: " + date + "\n"
                + "POST /v2/aisp HTTP/1.1";

        // HMAC-SHA256 签名
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        String signature = Base64.getEncoder().encodeToString(
                mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8)));

        // 组装 authorization 参数
        String authorizationOrigin = "api_key=\"" + apiKey
                + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\""
                + signature + "\"";
        String authorization = Base64.getEncoder().encodeToString(
                authorizationOrigin.getBytes(StandardCharsets.UTF_8));

        return ASR_URL + "?host=" + host + "&date=" + date + "&authorization=" + authorization;
    }

    /**
     * 构建讯飞语音听写请求体
     */
    private String buildRequestBody(String base64Audio) throws Exception {
        // 构建 common 参数
        String common = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("app_id", appId));

        // 构建 business 参数：开启方言识别（河北话）
        String business = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("language", "zh_cn")
                        .put("domain", "iat")
                        .put("accent", "hebei")
                        .put("vad_eos", 3000)     // 尾端点静音检测 3 秒
                        .put("dwa", "wpgs")        // 动态修正
        );

        // 构建 data 参数
        String data = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("status", 2)          // 2 = 最后一帧（一次性上传）
                        .put("format", "audio/L16;rate=16000")
                        .put("encoding", "raw")
                        .put("audio", base64Audio)
        );

        return objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("common", common)
                        .put("business", business)
                        .put("data", data)
        );
    }

    /**
     * 解析讯飞语音识别响应
     */
    private SpeechRecognitionResult parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // 检查 header.code
        JsonNode header = root.get("header");
        if (header == null) {
            throw new IOException("讯飞 ASR 响应格式异常：缺少 header");
        }
        int code = header.get("code").asInt();
        if (code != 0) {
            String message = header.has("message") ? header.get("message").asText() : "未知错误";
            throw new IOException("讯飞 ASR 错误 [" + code + "]: " + message);
        }

        // 解析识别结果
        JsonNode payload = root.get("payload");
        if (payload == null) {
            return new SpeechRecognitionResult("", "", 0.0, "hebei", "xunfei", 0);
        }

        JsonNode result = payload.get("result");
        if (result == null) {
            return new SpeechRecognitionResult("", "", 0.0, "hebei", "xunfei", 0);
        }

        // 拼接所有识别片段
        StringBuilder textBuilder = new StringBuilder();
        double totalConfidence = 0.0;
        int segmentCount = 0;

        JsonNode ws = result.get("ws");
        if (ws != null && ws.isArray()) {
            for (JsonNode wordSegment : ws) {
                JsonNode cw = wordSegment.get("cw");
                if (cw != null && cw.isArray()) {
                    for (JsonNode word : cw) {
                        if (word.has("w")) {
                            textBuilder.append(word.get("w").asText());
                        }
                    }
                }
            }
        }

        // 计算平均置信度
        if (result.has("rg")) {
            JsonNode rg = result.get("rg");
            if (rg.isArray() && !rg.isEmpty()) {
                for (JsonNode r : rg) {
                    totalConfidence += r.asDouble();
                    segmentCount++;
                }
            }
        }
        double confidence = segmentCount > 0 ? totalConfidence / segmentCount : 0.0;

        // 提取方言原文（讯飞有时返回 dialect 字段）
        String rawDialectText = "";
        if (result.has("dialect")) {
            rawDialectText = result.get("dialect").asText();
        }

        log.info("讯飞 ASR 识别完成: text={}, confidence={}, segments={}",
                textBuilder.toString(), confidence, segmentCount);

        return new SpeechRecognitionResult(
                textBuilder.toString().trim(),
                rawDialectText,
                Math.min(confidence, 1.0),
                "hebei",
                "xunfei",
                0  // 一次性上传模式无法精确获取时长
        );
    }
}
