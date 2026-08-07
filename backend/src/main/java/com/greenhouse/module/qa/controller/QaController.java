package com.greenhouse.module.qa.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.PageResult;
import com.greenhouse.module.qa.dto.QaAskRequest;
import com.greenhouse.module.qa.dto.QaHistoryItem;
import com.greenhouse.module.qa.dto.QaResponse;
import com.greenhouse.module.qa.service.RagQaService;
import com.greenhouse.module.qa.service.VoiceQaService;
import com.greenhouse.entity.User;
import com.greenhouse.repository.QaRecordRepository;
import com.greenhouse.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
 *   <li>GET /api/v1/qa/records — 问答历史（分页，可按日期过滤）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
public class QaController {

    private final RagQaService ragQaService;
    private final VoiceQaService voiceQaService;
    private final QaRecordRepository recordRepository;
    private final UserRepository userRepository;

    /**
     * 文字问答
     * POST /api/v1/qa/ask
     */
    @PostMapping("/ask")
    public ApiResponse<QaResponse> ask(
            @Valid @RequestBody QaAskRequest request) {

        Long userId = getCurrentUserId();
        assertNotWorker(userId);
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
        assertNotWorker(userId);
        QaResponse result = voiceQaService.askVoice(userId, greenhouseId, audioFile);

        return ApiResponse.success("语音识别完成", result);
    }

    /**
     * 问答历史
     * GET /api/v1/qa/records?page=1&size=30&date=2026-08-07
     * <p>
     * page 从 1 开始（默认 1）；size 默认 30，上限 100；
     * date 可选（格式 yyyy-MM-dd）：按当天 00:00 ~ 次日 00:00 过滤，不传则查询全部。
     * </p>
     */
    @GetMapping("/records")
    public ApiResponse<PageResult<QaHistoryItem>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Long userId = getCurrentUserId();
        assertNotWorker(userId);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100));

        Page<QaHistoryItem> items;
        if (date != null) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            items = recordRepository
                    .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, start, end, pageable)
                    .map(QaHistoryItem::fromEntity);
        } else {
            items = recordRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                    .map(QaHistoryItem::fromEntity);
        }

        return ApiResponse.success(PageResult.of(items));
    }

    // ===== 辅助方法 =====

    /**
     * R23：普通员工（WORKER）无 AI 问答/专家咨询权限，直接拒绝
     */
    private void assertNotWorker(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getRole() == User.Role.WORKER) {
                throw new com.greenhouse.common.BusinessException(
                        com.greenhouse.common.ErrorCode.FUNCTION_DENIED, "普通员工无 AI 问答权限");
            }
        });
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}