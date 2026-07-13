package com.greenhouse.module.diagnosis.service;

import com.greenhouse.ai.DiseaseRecognitionProvider;
import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.DiagnosticRecord;
import com.greenhouse.module.diagnosis.dto.DiagnosisResponse;
import com.greenhouse.module.file.service.FileService;
import com.greenhouse.repository.DiagnosticRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 病虫害诊断服务
 * <p>
 * 核心流程：接收图片 → 保存文件 → 调用 AI 识别 → 保存诊断记录 → 返回结果。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiseaseRecognitionProvider recognitionProvider;
    private final DiagnosticRecordRepository recordRepository;
    private final FileService fileService;

    /**
     * 执行图片诊断
     *
     * @param userId       用户ID
     * @param greenhouseId 大棚ID（可空）
     * @param imageFile    上传的图片文件
     * @return 诊断结果
     */
    @Transactional
    public DiagnosisResponse diagnose(Long userId, Long greenhouseId, MultipartFile imageFile) {
        // 1. 保存图片文件
        String imagePath = fileService.saveDiagnosisImage(imageFile);

        // 2. 调用 AI 识别
        DiseaseRecognitionProvider.RecognitionResult aiResult;
        try {
            aiResult = recognitionProvider.recognize(imageFile.getBytes());
        } catch (Exception e) {
            log.error("AI 识别失败: {}", e.getMessage(), e);

            // 即使识别失败也保存记录
            DiagnosticRecord failRecord = DiagnosticRecord.builder()
                    .userId(userId)
                    .greenhouseId(greenhouseId)
                    .imagePath(imagePath)
                    .diseaseName("识别失败")
                    .confidence(0.0)
                    .treatment("请重试或咨询专家")
                    .recognitionEngine("unknown")
                    .build();
            recordRepository.save(failRecord);

            throw new BusinessException(ErrorCode.AI_RECOGNITION_FAILED);
        }

        // 3. 保存诊断记录
        DiagnosticRecord record = DiagnosticRecord.builder()
                .userId(userId)
                .greenhouseId(greenhouseId)
                .imagePath(imagePath)
                .diseaseName(aiResult.diseaseName())
                .confidence(aiResult.confidence())
                .treatment(aiResult.treatment())
                .recognitionEngine(aiResult.engineName())
                .build();

        record = recordRepository.save(record);
        log.info("诊断完成: id={}, disease={}, confidence={}, needExpert={}",
                record.getId(), aiResult.diseaseName(), aiResult.confidence(),
                aiResult.needExpertConsultation());

        return DiagnosisResponse.fromEntity(record);
    }

    /**
     * 查询诊断历史
     */
    public List<DiagnosisResponse> getHistory(Long userId, int page, int size) {
        Page<DiagnosticRecord> records = recordRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        return records.getContent().stream()
                .map(DiagnosisResponse::fromEntity)
                .toList();
    }
}
