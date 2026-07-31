package com.greenhouse.module.growth.dto;

import com.greenhouse.entity.GrowthAssessment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 截帧图片响应 DTO
 */
@Data
@Builder
public class GrowthImageResponse {

    private Long id;
    private String imagePath;
    private String capturedAt;
    private String resolution;
    private long fileSize;

    public static GrowthImageResponse fromEntity(GrowthAssessment entity) {
        // 分辨率模拟值（后续接FFmpeg时填入真实值）
        return GrowthImageResponse.builder()
                .id(entity.getId())
                .imagePath(entity.getImagePath())
                .capturedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : "")
                .resolution("1920x1080")
                .fileSize(0)
                .build();
    }
}
