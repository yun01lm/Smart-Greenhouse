package com.greenhouse.ai;

import java.util.List;

/**
 * 语音识别策略接口
 * <p>
 * 提供统一的语音转文字能力，支持方言识别。
 * 初期使用讯飞 API，后期可切换为本地 Whisper 模型。
 * </p>
 *
 * <h3>实现类</h3>
 * <ul>
 *   <li>{@code XunfeiSpeechProvider} — 讯飞 ASR API（初期）</li>
 *   <li>{@code WhisperSpeechProvider} — 本地 Whisper 推理（后期）</li>
 * </ul>
 */
public interface SpeechRecognitionProvider {

    /**
     * 识别语音为文字
     *
     * @param audioData 音频字节数据（支持 wav/mp3/amr 格式）
     * @return 识别结果
     * @throws Exception 识别失败时抛出
     */
    SpeechRecognitionResult recognize(byte[] audioData) throws Exception;

    /**
     * 获取当前使用的识别引擎名称
     */
    String getEngineName();

    /**
     * 获取支持的方言列表
     */
    List<String> getSupportedDialects();

    /**
     * 语音识别结果
     */
    record SpeechRecognitionResult(
            /** 识别文本（普通话） */
            String text,
            /** 方言原文（可空，讯飞支持方言转写时填充） */
            String rawDialectText,
            /** 置信度 0.0-1.0 */
            double confidence,
            /** 方言类型，如 hebei */
            String dialect,
            /** 识别引擎名称：xunfei / whisper */
            String engineName,
            /** 音频时长（毫秒） */
            int durationMs
    ) {}
}
