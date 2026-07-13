package com.greenhouse.module.qa.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.PageResult;
import com.greenhouse.module.qa.dto.QaAskRequest;
import com.greenhouse.module.qa.dto.QaHistoryItem;
import com.greenhouse.module.qa.dto.QaResponse;
import com.greenhouse.module.qa.service.RagQaService;
import com.greenhouse.module.qa.service.VoiceQaService;
import com.greenhouse.repository.QaRecordRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 问答 API
 * <p>
 * 路径前缀：/api/v1/qa
 * </p>
 *
 * <h3>端点列表</h3>
 * <ul>
 *   <li>POST /api/v1/qa/ask — 文字问答</li>
 *   <li>POST /api/v1/qa/ask/voice — 语音问答（multipart/form-data）</li>
 *   <li>GET /api/v1/qa/records — 问答历史（分页）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
public class QaController {

    private final RagQaService ragQaService;
    private final VoiceQaService voiceQaService;
    private final QaRecordRepository recordRepository;

    /**
     * 文字问答
     * POST /api/v1/qa/ask
     */
    @PostMapping("/ask")
    public ApiResponse<QaResponse> ask(
            @Valid @RequestBody QaAskRequest request) {

        Long userId = getCurrentUserId();
        QaResponse result = ragQaService.askText(
                userId, request.getQuestion(), request.getGreenhouseId());

        return ApiResponse.success(result);
    }

    /**
     * 语音问答
     * POST /api/v1/qa/ask/voice
     *
     * @param audioFile    音频文件（multipart/form-data，字段名 audio）
     * @param greenhouseId 关联大棚ID（可选）
     */
    @PostMapping("/ask/voice")
    public ApiResponse<QaResponse> askVoice(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(required = false) Long greenhouseId) {

        Long userId = getCurrentUserId();
        QaResponse result = voiceQaService.askVoice(userId, greenhouseId, audioFile);

        return ApiResponse.success("语音识别完成", result);
    }

    /**
     * 问答历史
     * GET /api/v1/qa/records?page=0&size=10
     */
    @GetMapping("/records")
    public ApiResponse<PageResult<QaHistoryItem>> records(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = getCurrentUserId();
        Page<QaHistoryItem> items = recordRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(QaHistoryItem::fromEntity);

        return ApiResponse.success(PageResult.of(items));
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
