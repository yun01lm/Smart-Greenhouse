package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 方言语料实体
 * <p>
 * 对应数据库 dialect_corpus 表。存储方言音频文件和对应的标注文本，
 * 用于方言语料库的积累和管理。
 * </p>
 */
@Entity
@Table(name = "dialect_corpus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DialectCorpus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 方言类型：hebei / shandong / dongbei / ... */
    @Column(name = "dialect", nullable = false, length = 30)
    private String dialect;

    /** 音频文件路径（服务器本地路径） */
    @Column(name = "audio_path", nullable = false, length = 500)
    private String audioPath;

    /** 原始音频文件名 */
    @Column(name = "audio_filename", nullable = false, length = 255)
    private String audioFilename;

    /** 音频文件大小（字节） */
    @Column(name = "audio_size")
    private Long audioSize;

    /** 标注文本（标准普通话转写） */
    @Column(name = "annotation_text", columnDefinition = "TEXT")
    private String annotationText;

    /** 方言原文（可选，方言口音下的原始文本） */
    @Column(name = "dialect_text", columnDefinition = "TEXT")
    private String dialectText;

    /** 来源：MANUAL（手动上传）/ QA_COLLECT（语音问答采集） */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String source = "MANUAL";

    /** 备注 */
    @Column(length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
