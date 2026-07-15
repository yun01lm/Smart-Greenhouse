package com.greenhouse.ai.mock;

import com.greenhouse.ai.SpeechRecognitionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mock 语音识别 Provider
 * <p>
 * 验收时使用，返回模拟的语音转文字结果，不调用真实 API。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.voice.provider", havingValue = "mock")
public class MockSpeechRecognitionProvider implements SpeechRecognitionProvider {

    @Override
    public SpeechRecognitionResult recognize(byte[] audioData) throws Exception {
        log.info("[MOCK] 语音识别: 返回模拟结果 (audioSize={} bytes)", audioData.length);
        Thread.sleep(300);
        return new SpeechRecognitionResult(
                "大棚温度有点高，请帮我打开风机降温。",
                "大棚温度有点儿高，帮俺开开风机降降温。",
                0.92,
                "hebei",
                "mock-speech",
                audioData.length / 16  // 粗略估算时长
        );
    }

    @Override
    public String getEngineName() {
        return "mock-speech";
    }

    @Override
    public List<String> getSupportedDialects() {
        return List.of("hebei", "shandong", "dongbei", "henan", "sichuan");
    }
}
