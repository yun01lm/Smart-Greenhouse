package com.greenhouse.module.admin.dto;

import com.greenhouse.entity.DialectCorpus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 方言语料响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DialectCorpusResponse {

    private Long id;
    private String dialect;
    private String audioFilename;
    private Long audioSize;
    private String audioUrl;
    private String annotationText;
    private String dialectText;
    private String source;
    private String remark;
    private LocalDateTime createdAt;

    public static DialectCorpusResponse fromEntity(DialectCorpus c) {
        return DialectCorpusResponse.builder()
                .id(c.getId())
                .dialect(c.getDialect())
                .audioFilename(c.getAudioFilename())
                .audioSize(c.getAudioSize())
                .audioUrl("/api/v1/admin/corpus/" + c.getId() + "/audio")
                .annotationText(c.getAnnotationText())
                .dialectText(c.getDialectText())
                .source(c.getSource())
                .remark(c.getRemark())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
