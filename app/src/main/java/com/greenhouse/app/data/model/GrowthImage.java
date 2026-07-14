package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 截帧图片模型
 * <p>
 * 对应后端 GET /api/v1/growth/images 的响应数据。
 * 由 FFmpeg 从 ESP32-CAM RTSP 推流中定时截取（每30分钟）。
 * </p>
 */
public class GrowthImage {

    private long id;

    @SerializedName("imagePath")
    private String imagePath;

    @SerializedName("capturedAt")
    private String capturedAt;

    @SerializedName("resolution")
    private String resolution;

    @SerializedName("fileSize")
    private long fileSize;

    // ===== Getter =====

    public long getId() { return id; }
    public String getImagePath() { return imagePath; }
    public String getCapturedAt() { return capturedAt; }
    public String getResolution() { return resolution; }
    public long getFileSize() { return fileSize; }

    // ===== 辅助方法 =====

    /**
     * 获取文件大小显示文本
     */
    public String getFileSizeText() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * 是否有图片
     */
    public boolean hasImage() {
        return imagePath != null && !imagePath.isEmpty();
    }
}
