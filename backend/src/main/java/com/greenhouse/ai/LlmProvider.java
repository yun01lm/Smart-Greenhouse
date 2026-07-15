package com.greenhouse.ai;

/**
 * LLM 大语言模型策略接口
 * <p>
 * 提供统一的文本生成能力，支持 DeepSeek 真实调用和 Mock 模式。
 * 用于 RAG 问答、诊断建议生成等场景。
 * </p>
 */
public interface LlmProvider {

    /**
     * 生成文本回答
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户问题
     * @param temperature  温度参数（0.0-1.0）
     * @param maxTokens    最大输出 token 数
     * @return 生成的回答文本
     * @throws Exception 调用失败时抛出
     */
    String generate(String systemPrompt, String userMessage, double temperature, int maxTokens) throws Exception;

    /**
     * 获取当前引擎名称
     */
    String getEngineName();
}
