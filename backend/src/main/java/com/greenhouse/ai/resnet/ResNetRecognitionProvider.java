package com.greenhouse.ai.resnet;

import com.greenhouse.ai.DiseaseRecognitionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * ResNet 本地模型识别实现（后期）
 * <p>
 * Phase 3 阶段实现，使用本地训练好的 ResNet 模型进行推理。
 * 当前为空壳实现，默认使用百度 AI。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.image.provider", havingValue = "resnet")
public class ResNetRecognitionProvider implements DiseaseRecognitionProvider {

    @Override
    public RecognitionResult recognize(byte[] imageBytes) throws Exception {
        log.warn("ResNet 识别引擎尚未实现，请切换到百度 AI");
        throw new UnsupportedOperationException("ResNet 识别引擎尚未实现，Phase 3 阶段开发");
    }
}
