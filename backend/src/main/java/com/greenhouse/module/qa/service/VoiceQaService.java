package com.greenhouse.module.qa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.ai.SpeechRecognitionProvider;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.QaRecord;
import com.greenhouse.module.file.service.FileService;
import com.greenhouse.module.qa.dto.QaResponse;
import com.greenhouse.repository.QaRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 语音问答服务
 * <p>
 * 链路：音频文件 → 语音识别(ASR) → RAG 问答 → 保存记录 → 返回结果。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceQaService {

    private final SpeechRecognitionProvider speechProvider;
    private final RagQaService ragQaService;
    private final QaRecordRepository recordRepository;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    /**
     * 语音问答
     *
     * @param userId       用户ID
     * @param greenhouseId 大棚ID（可空）
     * @param audioFile    音频文件
     * @return 问答结果（含语音识别文本）
     */
    @Transactional
    public QaResponse askVoice(Long userId, Long greenhouseId, MultipartFile audioFile) {
        // 1. 保存音频文件
        String audioPath = fileService.saveAudioFile(audioFile);

        // 2. 语音识别
        SpeechRecognitionProvider.SpeechRecognitionResult asrResult;
        try {
            asrResult = speechProvider.recognize(audioFile.getBytes());
        } catch (Exception e) {
            log.error("语音识别失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SPEECH_FAILED);
        }

        String question = asrResult.text();
        if (question == null || question.isBlank()) {
            throw new BusinessException(ErrorCode.AI_SPEECH_FAILED);
        }

        log.info("语音识别完成: text={}, engine={}, dialect={}, confidence={}",
                question, asrResult.engineName(), asrResult.dialect(), asrResult.confidence());

        // 3. RAG 问答（仅生成回答，不保存记录）
        String answer;
        String sourcesJson = "[]";
        try {
            RagQaService.AnswerResult result = ragQaService.generateAnswerOnly(question);
            answer = result.answer();
            sourcesJson = buildSourcesJson(result);
        } catch (BusinessException e) {
            // 即使 LLM 失败也保存语音识别结果
            log.error("RAG 问答失败: {}", e.getMessage());
            answer = "抱歉，AI 服务暂时不可用，请稍后重试。";
        }

        // 4. 保存语音问答记录
        QaRecord record = QaRecord.builder()
                .userId(userId)
                .question(question)
                .answer(answer)
                .inputType(QaRecord.InputType.VOICE)
                .asrEngine(asrResult.engineName())
                .sources(sourcesJson)
                .createdAt(LocalDateTime.now())
                .build();
        record = recordRepository.save(record);

        log.info("语音问答完成: id={}, question_length={}, engine={}",
                record.getId(), question.length(), asrResult.engineName());

        return QaResponse.fromVoiceEntity(record, asrResult.dialect());
    }

    /**
     * 构建引用来源 JSON
     */
    private String buildSourcesJson(RagQaService.AnswerResult result) {
        try {
            var array = objectMapper.createArrayNode();
            for (QaResponse.SourceInfo source : result.sources()) {
                var node = objectMapper.createObjectNode();
                node.put("title", source.getTitle());
                node.put("category", source.getCategory());
                array.add(node);
            }
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            return "[]";
        }
    }
}
