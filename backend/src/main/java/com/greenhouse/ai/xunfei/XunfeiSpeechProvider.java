package com.greenhouse.ai.xunfei;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenhouse.ai.SpeechRecognitionProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ????????
 * <p>
 * ?????????? WebAPI?WebSocket ????????????????????
 * ?? HMAC-SHA256 ???????
 * </p>
 *
 * <h3>API ??</h3>
 * <ul>
 *   <li>WebAPI ???https://www.xfyun.cn/doc/asr/voicedictation/API.html</li>
 *   <li>?????hebei???????????????????</li>
 * </ul>
 *
 * <h3>???</h3>
 * <ul>
 *   <li>{@code xunfei.app-id} ? ???? ID</li>
 *   <li>{@code xunfei.api-key} ? ?? API Key</li>
 *   <li>{@code xunfei.api-secret} ? ?? API Secret??????</li>
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

    /** ???????? WebAPI ???WebSocket ??? */
    private static final String ASR_URL = "wss://iat-api.xfyun.cn/v2/iat";

    /** ???????????? host ????? */
    private static final String HOST = "iat-api.xfyun.cn";

    /** ???????????????? 8000 ??? */
    private static final int FRAME_SIZE = 8000;

    /** ????? */
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
                .readTimeout(30, TimeUnit.SECONDS)
                .pingInterval(5, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public SpeechRecognitionResult recognize(byte[] audioData) throws Exception {
        String requestUrl = buildAuthUrl();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SpeechRecognitionResult> resultRef = new AtomicReference<>();
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        Request request = new Request.Builder().url(requestUrl).build();
        WebSocketListener listener = new WebSocketListener() {
            private final StringBuilder textBuilder = new StringBuilder();

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                sendFrames(webSocket, audioData);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonNode root = objectMapper.readTree(text);
                    int code = root.path("code").asInt(-1);
                    if (code != 0) {
                        String message = root.path("message").asText("????");
                        errorRef.set(new IOException("?? ASR ?? [" + code + "]: " + message));
                        latch.countDown();
                        return;
                    }
                    JsonNode ws = root.path("data").path("result").path("ws");
                    if (ws.isArray()) {
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
                    int status = root.path("data").path("status").asInt(-1);
                    if (status == 2) {
                        resultRef.set(new SpeechRecognitionResult(
                                textBuilder.toString().trim(), "", 0.0, "hebei", "xunfei", 0));
                        latch.countDown();
                    }
                } catch (Exception e) {
                    errorRef.set(e);
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                errorRef.set(new IOException("?? WebSocket ????: " + t.getMessage(), t));
                latch.countDown();
            }
        };

        WebSocket webSocket = httpClient.newWebSocket(request, listener);
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new IOException("?? ASR ??");
            }
        } finally {
            webSocket.close(1000, null);
            httpClient.dispatcher().executorService().shutdown();
        }

        if (errorRef.get() != null) {
            throw errorRef.get();
        }
        return resultRef.get();
    }

    /** ???????status 0 ??? common/business?1 ????2 ??? */
    private void sendFrames(WebSocket webSocket, byte[] audioData) {
        try {
            int offset = 0;
            int frameIndex = 0;
            int total = audioData.length;
            while (offset < total) {
                int len = Math.min(FRAME_SIZE, total - offset);
                byte[] chunk = new byte[len];
                System.arraycopy(audioData, offset, chunk, 0, len);
                String frame;
                if (frameIndex == 0) {
                    frame = buildFirstFrame(chunk);
                } else {
                    frame = buildMiddleFrame(chunk, 1);
                }
                log.debug("?????[{}] len={}: {}", frameIndex, frame.length(), frame.substring(0, Math.min(200, frame.length())));
                webSocket.send(frame);
                offset += len;
                frameIndex++;
            }
            String last = buildMiddleFrame(new byte[0], 2);
            log.debug("??????: {}", last);
            webSocket.send(last);
        } catch (Exception e) {
            log.error("???????: {}", e.getMessage(), e);
        }
    }

    private String buildFirstFrame(byte[] chunk) throws Exception {
        String common = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("app_id", appId));
        String business = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("language", "zh_cn")
                        .put("domain", "iat")
                        .put("accent", "hebei")
                        .put("vad_eos", 3000)
                        .put("dwa", "wpgs")
                        .put("vinfo", 1));
        String data = buildDataFrame(0, chunk);
        ObjectNode frame = objectMapper.createObjectNode();
        frame.set("common", objectMapper.readTree(common));
        frame.set("business", objectMapper.readTree(business));
        frame.set("data", objectMapper.readTree(data));
        return objectMapper.writeValueAsString(frame);
    }

    private String buildMiddleFrame(byte[] chunk, int status) throws Exception {
        String dataJson = buildDataFrame(status, chunk);
        ObjectNode frame = objectMapper.createObjectNode();
        frame.set("data", objectMapper.readTree(dataJson));
        return objectMapper.writeValueAsString(frame);
    }

    private String buildDataFrame(int status, byte[] chunk) throws Exception {
        String base64Audio = Base64.getEncoder().encodeToString(chunk);
        return objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("status", status)
                        .put("format", "audio/L16;rate=16000")
                        .put("encoding", "raw")
                        .put("audio", base64Audio));
    }

    @Override
    public String getEngineName() {
        return "xunfei";
    }

    @Override
    public List<String> getSupportedDialects() {
        return SUPPORTED_DIALECTS;
    }

    // ===== ???? =====

    /**
     * ??? HMAC-SHA256 ??? WebSocket ?? URL
     * <p>
     * ???????? host + date + request-line?GET /v2/iat HTTP/1.1?? HMAC-SHA256 ???
     * ????? authorization ?????
     * </p>
     */
    private String buildAuthUrl() throws Exception {
        String host = HOST;

        // RFC 1123 ????
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        // ?????: host: host\ndate: date\nGET /v2/iat HTTP/1.1
        String signatureOrigin = "host: " + host + "\ndate: " + date + "\n"
                + "GET /v2/iat HTTP/1.1";

        // HMAC-SHA256 ??
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        String signature = Base64.getEncoder().encodeToString(
                mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8)));

        // ?? authorization ??
        String authorizationOrigin = "api_key=\"" + apiKey
                + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\""
                + signature + "\"";
        String authorization = Base64.getEncoder().encodeToString(
                authorizationOrigin.getBytes(StandardCharsets.UTF_8));

        return ASR_URL + "?authorization=" + encode(authorization)
                + "&date=" + encode(date)
                + "&host=" + encode(host);
    }

    /** URL ?????? %20 ????????????? */
    private String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
