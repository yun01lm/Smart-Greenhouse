package com.greenhouse.ai;

/**
 * Embedding 向量化策略接口
 * <p>
 * 提供统一的文本向量化能力，支持 SiliconFlow 真实调用和 Mock 模式。
 * </p>
 */
public interface EmbeddingProvider {

    /**
     * 单条文本向量化
     *
     * @param text 文本内容
     * @return 向量（浮点数组）
     * @throws Exception 调用失败时抛出
     */
    float[] embed(String text) throws Exception;

    /**
     * 批量文本向量化
     *
     * @param texts 文本列表
     * @return 向量列表
     * @throws Exception 调用失败时抛出
     */
    java.util.List<float[]> embedBatch(java.util.List<String> texts) throws Exception;

    /**
     * 获取向量维度
     */
    int getDimension();

    /**
     * 获取当前引擎名称
     */
    String getEngineName();
}
