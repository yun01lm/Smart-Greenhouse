package com.greenhouse.ai.whisper;

import com.greenhouse.ai.SpeechRecognitionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Whisper 本地语音识别占位实现
 * <p>
 * Phase 3 阶段将替换为真正的 Whisper 模型推理。
 * 目前直接抛出 UnsupportedOperationException，由全局异常处理返回友好提示。
 * </p>
 *
 * <h3>后期实现方案</h3>
 * <ul>
 *   <li>使用 DJL (Deep Java Library) 加载 Whisper ONNX 模型</li>
 *   <li>音频预处理：16kHz 重采样 → Mel 频谱 → 模型推理</li>
 *   <li>河北方言微调模型通过独立训练流程产出</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.voice.provider", havingValue = "whisper")
public class WhisperSpeechProvider implements SpeechRecognitionProvider {

    @Override
    public SpeechRecognitionResult recognize(byte[] audioData) throws Exception {
        log.warn("Whisper 语音识别尚未实现，Phase 3 阶段启用");
        throw new UnsupportedOperationException(
                "本地 Whisper 模型尚未部署，请切换至讯飞 API（ai.voice.provider=xunfei）");
    }

    @Override
    public String getEngineName() {
        return "whisper";
    }

    @Override
    public List<String> getSupportedDialects() {
        return List.of("mandarin", "hebei", "cantonese", "sichuanese");
    }
}
