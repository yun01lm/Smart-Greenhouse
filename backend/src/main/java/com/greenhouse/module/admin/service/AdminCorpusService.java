package com.greenhouse.module.admin.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.DialectCorpus;
import com.greenhouse.module.admin.dto.DialectCorpusResponse;
import com.greenhouse.repository.DialectCorpusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 管理员方言语料管理服务
 * <p>
 * 支持语料上传（音频+标注文本）、列表查询（方言筛选+关键词搜索+分页）、删除（含音频文件清理）。
 * 语料用于方言语料库积累，后续支撑方言识别模型微调。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCorpusService {

    private final DialectCorpusRepository corpusRepository;

    @Value("${greenhouse.upload.path:uploads}")
    private String uploadRoot;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final long MAX_AUDIO_SIZE = 30 * 1024 * 1024; // 30MB
    private static final String[] ALLOWED_AUDIO_TYPES = {"audio/wav", "audio/mpeg", "audio/mp3",
            "audio/webm", "audio/amr", "audio/x-wav", "audio/wave"};

    /**
     * 上传语料
     */
    @Transactional
    public DialectCorpusResponse upload(MultipartFile audio, String dialect,
                                         String annotationText, String dialectText,
                                         String source, String remark) {
        // 校验音频文件
        if (audio == null || audio.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "音频文件不能为空");
        }
        if (audio.getSize() > MAX_AUDIO_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "音频文件大小不能超过 30MB");
        }

        String contentType = audio.getContentType();
        boolean allowed = false;
        for (String t : ALLOWED_AUDIO_TYPES) {
            if (t.equals(contentType)) { allowed = true; break; }
        }
        if (!allowed) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的音频格式，支持: wav, mp3, webm, amr");
        }

        // 保存音频文件
        String dateDir = LocalDate.now().format(DATE_FMT);
        String ext = getFileExtension(audio.getOriginalFilename());
        String storedName = UUID.randomUUID().toString() + ext;
        // 必须使用绝对路径：MultipartFile.transferTo 对相对路径会按 Tomcat 工作目录解析，
        // 导致保存到临时目录下而失败（此前语料上传因此不可用）
        Path dir = Paths.get(uploadRoot).toAbsolutePath().resolve("corpus").resolve(dateDir);
        try {
            Files.createDirectories(dir);
            Path filePath = dir.resolve(storedName);
            audio.transferTo(filePath.toFile());

            // 创建数据库记录
            DialectCorpus corpus = DialectCorpus.builder()
                    .dialect(dialect)
                    .audioPath(filePath.toString())
                    .audioFilename(audio.getOriginalFilename())
                    .audioSize(audio.getSize())
                    .annotationText(annotationText)
                    .dialectText(dialectText)
                    .source(source != null ? source : "MANUAL")
                    .remark(remark)
                    .build();

            corpus = corpusRepository.save(corpus);
            log.info("语料上传成功: id={}, dialect={}, filename={}",
                    corpus.getId(), dialect, audio.getOriginalFilename());

            return DialectCorpusResponse.fromEntity(corpus);
        } catch (IOException e) {
            log.error("语料文件保存失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
    }

    /**
     * 语料列表（分页 + 筛选）
     */
    public Page<DialectCorpusResponse> list(String dialect, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DialectCorpus> result;

        if (dialect != null && !dialect.isEmpty() && keyword != null && !keyword.isEmpty()) {
            result = corpusRepository.findByDialectAndAnnotationTextContaining(dialect, keyword, pageable);
        } else if (dialect != null && !dialect.isEmpty()) {
            result = corpusRepository.findByDialect(dialect, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            result = corpusRepository.findByAnnotationTextContaining(keyword, pageable);
        } else {
            result = corpusRepository.findAll(pageable);
        }

        return result.map(DialectCorpusResponse::fromEntity);
    }

    /**
     * 获取所有方言类型
     */
    public List<String> getDialects() {
        return corpusRepository.findDistinctDialects();
    }

    /**
     * 删除语料（含音频文件）
     */
    @Transactional
    public void delete(Long id) {
        DialectCorpus corpus = corpusRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "语料记录不存在"));

        // 删除音频文件
        try {
            Path audioFile = Paths.get(corpus.getAudioPath());
            Files.deleteIfExists(audioFile);
            log.info("语料音频文件已删除: {}", corpus.getAudioPath());
        } catch (IOException e) {
            log.warn("语料音频文件删除失败（可能已不存在）: {}", corpus.getAudioPath(), e);
        }

        corpusRepository.delete(corpus);
        log.info("语料记录已删除: id={}", id);
    }

    /**
     * 解析语料音频文件的服务器路径（用于播放/下载）
     */
    public Path resolveAudioPath(Long id) {
        DialectCorpus corpus = corpusRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "语料记录不存在"));
        Path path = Paths.get(corpus.getAudioPath());
        if (!path.isAbsolute()) {
            path = Paths.get(uploadRoot).toAbsolutePath().resolve(path);
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "音频文件不存在或已被删除");
        }
        return path;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
