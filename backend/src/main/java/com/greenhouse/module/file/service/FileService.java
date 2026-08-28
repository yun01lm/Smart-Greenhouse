package com.greenhouse.module.file.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储服务
 * <p>
 * 负责图片等文件的上传和本地存储管理。
 * 文件按日期分目录：uploads/diagnosis/2026/07/13/uuid.jpg
 * </p>
 */
@Slf4j
@Service
public class FileService {

    private final Path uploadDir;

    /** 允许的最大文件大小 */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    /** 音频文件最大大小 */
    private static final long MAX_AUDIO_SIZE = 30 * 1024 * 1024; // 30MB
    /** 允许的音频类型（含 APP 语音问答的 PCM 16k 裸流，第 10 项修复） */
    private static final String[] ALLOWED_AUDIO_TYPES = {"audio/wav", "audio/mpeg", "audio/mp3", "audio/amr", "audio/webm", "audio/x-pcm", "audio/pcm"};

    public FileService(@Value("${file.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", this.uploadDir, e);
        }
    }

    /**
     * 保存诊断图片
     *
     * @param file 上传的文件
     * @return 文件访问路径（相对路径）
     */
    public String saveDiagnosisImage(MultipartFile file) {
        // 校验文件
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        // 生成文件名和路径
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        Path targetDir = uploadDir.resolve("diagnosis").resolve(datePath);
        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            file.transferTo(targetFile.toFile());

            String relativePath = "diagnosis/" + datePath + "/" + filename;
            log.info("诊断图片已保存: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 获取文件的绝对路径
     */
    public Path getAbsolutePath(String relativePath) {
        return uploadDir.resolve(relativePath);
    }

    /**
     * 保存语音问答音频文件
     *
     * @param file 上传的音频文件
     * @return 文件访问路径（相对路径）
     */
    public String saveAudioFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "音频文件不能为空");
        }
        if (file.getSize() > MAX_AUDIO_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // 校验音频类型
        String contentType = file.getContentType();
        boolean validType = false;
        if (contentType != null) {
            for (String allowed : ALLOWED_AUDIO_TYPES) {
                if (contentType.equalsIgnoreCase(allowed)) {
                    validType = true;
                    break;
                }
            }
        }
        // 也允许通过扩展名判断
        if (!validType) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null) {
                String lower = originalFilename.toLowerCase();
                if (lower.endsWith(".wav") || lower.endsWith(".mp3")
                        || lower.endsWith(".amr") || lower.endsWith(".webm")) {
                    validType = true;
                }
            }
        }
        if (!validType) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        // 生成文件名和路径
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String originalFilename = file.getOriginalFilename();
        String extension = ".wav";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        Path targetDir = uploadDir.resolve("audio").resolve(datePath);
        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            file.transferTo(targetFile.toFile());

            String relativePath = "audio/" + datePath + "/" + filename;
            log.info("音频文件已保存: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("音频文件保存失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}
