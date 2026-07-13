package com.greenhouse.module.diagnosis.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.module.diagnosis.dto.DiagnosisResponse;
import com.greenhouse.module.diagnosis.service.DiagnosisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 病虫害诊断 API
 * <p>
 * 路径前缀：/api/v1/diagnosis
 * </p>
 */
@RestController
@RequestMapping("/api/v1/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    /**
     * 上传图片进行病虫害识别
     * POST /api/v1/diagnosis/recognize
     *
     * @param imageFile    图片文件（multipart/form-data，字段名 image）
     * @param greenhouseId 关联的大棚ID（可选）
     */
    @PostMapping("/recognize")
    public ApiResponse<DiagnosisResponse> recognize(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(required = false) Long greenhouseId) {

        Long userId = getCurrentUserId();
        DiagnosisResponse result = diagnosisService.diagnose(userId, greenhouseId, imageFile);

        String msg = result.getNeedExpert()
                ? "识别完成，但置信度较低，建议咨询专家"
                : "识别完成";
        return ApiResponse.success(msg, result);
    }

    /**
     * 查询诊断历史
     * GET /api/v1/diagnosis/records?page=0&size=20
     */
    @GetMapping("/records")
    public ApiResponse<List<DiagnosisResponse>> records(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();
        return ApiResponse.success(diagnosisService.getHistory(userId, page, size));
    }

    // ===== 辅助方法 =====

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }
}
