package com.greenhouse.module.knowledge.dto;

import com.greenhouse.entity.KnowledgeDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentResponse {

    private Long id;
    private String docNo;
    private String title;
    private String category;
    private String description;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private Integer chunkCount;
    private Boolean vectorIndexed;
    private LocalDateTime indexedAt;
    private LocalDateTime createdAt;

    /** 文件大小可读格式 */
    private String fileSizeFormatted;

    public static KnowledgeDocumentResponse fromEntity(KnowledgeDocument doc) {
        return KnowledgeDocumentResponse.builder()
                .id(doc.getId())
                .docNo(doc.getDocNo())
                .title(doc.getTitle())
                .category(doc.getCategory())
                .description(doc.getDescription())
                .filePath(doc.getFilePath())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .chunkCount(doc.getChunkCount())
                .vectorIndexed(doc.getVectorIndexed())
                .indexedAt(doc.getIndexedAt())
                .createdAt(doc.getCreatedAt())
                .fileSizeFormatted(formatFileSize(doc.getFileSize()))
                .build();
    }

    private static String formatFileSize(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
