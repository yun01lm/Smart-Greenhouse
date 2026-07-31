package com.greenhouse.module.admin.controller;

import com.greenhouse.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 引擎配置 API
 * <p>
 * 路径前缀：/api/v1/admin/ai<br>
 * 仅 ADMIN 角色可访问（由 SecurityConfig 的 /api/v1/admin/** 规则控制）。
 * </p>
 */
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    @Value("${ai.image.provider:baidu}")
    private String imageProvider;

    @Value("${ai.voice.provider:xunfei}")
    private String voiceProvider;

    @Value("${ai.llm.provider:deepseek}")
    private String llmProvider;

    @Value("${ai.embedding.provider:siliconflow}")
    private String embeddingProvider;

    /**
     * 获取当前 AI 引擎配置
     * GET /api/v1/admin/ai/config
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("imageProvider", imageProvider);
        config.put("voiceProvider", voiceProvider);
        config.put("llmProvider", llmProvider);
        config.put("embeddingProvider", embeddingProvider);

        config.put("availableImageProviders", List.of("baidu", "resnet", "mock"));
        config.put("availableVoiceProviders", List.of("xunfei", "whisper", "mock"));
        config.put("availableLlmProviders", List.of("deepseek"));
        config.put("availableEmbeddingProviders", List.of("siliconflow", "mock"));

        return ApiResponse.success(config);
    }

    /**
     * 获取各引擎状态和调用量
     * GET /api/v1/admin/ai/status
     * <p>
     * 当前返回各 Provider 的基本状态。调用量统计为占位值，
     * 后续可接入 Micrometer Metrics 实现真实计数。
     * </p>
     */
    @GetMapping("/status")
    public ApiResponse<List<Map<String, Object>>> getStatus() {
        List<Map<String, Object>> statuses = List.of(
                createStatus("image", imageProvider, "正常", 0),
                createStatus("voice", voiceProvider, "正常", 0),
                createStatus("llm", llmProvider, "正常", 0),
                createStatus("embedding", embeddingProvider, "正常", 0)
        );
        return ApiResponse.success(statuses);
    }

    private Map<String, Object> createStatus(String type, String provider, String status, long callCount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("provider", provider);
        m.put("status", status);
        m.put("callCount", callCount);
        return m;
    }
}
