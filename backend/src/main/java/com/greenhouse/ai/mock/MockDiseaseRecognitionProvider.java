package com.greenhouse.ai.mock;

import com.greenhouse.ai.DiseaseRecognitionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 病虫害识别 Provider
 * <p>
 * 验收时使用，返回模拟的病虫害诊断结果，不调用真实 API。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.image.provider", havingValue = "mock")
public class MockDiseaseRecognitionProvider implements DiseaseRecognitionProvider {

    @Override
    public RecognitionResult recognize(byte[] imageBytes) throws Exception {
        log.info("[MOCK] 病虫害识别: 返回模拟结果 (imageSize={} bytes)", imageBytes.length);
        // 模拟 200ms 处理延迟
        Thread.sleep(200);
        return new RecognitionResult(
                "番茄晚疫病（Mock）",
                0.85,
                "【农业防治】及时清除病残体，合理轮作。\n【化学防治】使用霜脲·锰锌或烯酰吗啉喷雾防治，7-10天一次。\n【物理防治】加强通风，降低棚内湿度。",
                "mock-recognition"
        );
    }
}
