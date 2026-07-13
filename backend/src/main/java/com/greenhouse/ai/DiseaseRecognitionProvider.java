package com.greenhouse.ai;

/**
 * 病虫害图像识别策略接口
 * <p>
 * 使用策略模式（Strategy Pattern），支持多种图像识别引擎切换。
 * 初期使用百度 AI 开放平台，后期可切换到本地 ResNet 模型。
 * </p>
 *
 * <h3>实现类</h3>
 * <ul>
 *   <li>{@code BaiduRecognitionProvider} — 百度 AI 植物识别（初期）</li>
 *   <li>{@code ResNetRecognitionProvider} — 本地 ResNet 模型（后期）</li>
 * </ul>
 */
public interface DiseaseRecognitionProvider {

    /**
     * 识别图片中的病虫害
     *
     * @param imageBytes 图片字节数据
     * @return 识别结果（JSON 格式，包含病虫害名称、置信度、防治方案）
     * @throws Exception 识别失败时抛出异常
     */
    RecognitionResult recognize(byte[] imageBytes) throws Exception;

    /**
     * 识别结果
     */
    record RecognitionResult(
            /** 病虫害名称 */
            String diseaseName,
            /** 置信度（0.0 ~ 1.0） */
            double confidence,
            /** 防治方案/建议 */
            String treatment,
            /** 识别引擎名称 */
            String engineName
    ) {
        /** 是否需要专家介入（置信度 < 70%） */
        public boolean needExpertConsultation() {
            return confidence < 0.70;
        }
    }
}
