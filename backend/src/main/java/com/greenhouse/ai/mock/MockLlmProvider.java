package com.greenhouse.ai.mock;

import com.greenhouse.ai.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock LLM Provider
 * <p>
 * 验收时使用，返回模拟的 AI 回答，不调用 DeepSeek API。
 * 模拟回答内容与问题相关，体现 RAG 效果。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmProvider implements LlmProvider {

    @Override
    public String generate(String systemPrompt, String userMessage,
                           double temperature, int maxTokens) throws Exception {
        log.info("[MOCK] LLM 生成: userMessage={}", userMessage.length() > 50
                ? userMessage.substring(0, 50) + "..." : userMessage);
        Thread.sleep(500);

        if (userMessage.contains("温度") || userMessage.contains("temp")) {
            return "【Mock 回答】根据当前大棚数据，温度维持在 25-28°C 之间，属于番茄生长的适宜温度范围。"
                    + "建议继续保持当前通风策略，注意午间高温时段适当增加通风量。";
        }
        if (userMessage.contains("病害") || userMessage.contains("病") || userMessage.contains("虫")) {
            return "【Mock 回答】根据知识库检索结果，番茄常见病害包括晚疫病、早疫病、灰霉病等。"
                    + "预防措施：合理轮作、加强通风、控制湿度。发现病株应及时清除并使用对应药剂防治。";
        }
        if (userMessage.contains("施肥") || userMessage.contains("肥料")) {
            return "【Mock 回答】根据作物生长阶段（开花期），建议施用高磷钾复合肥，氮肥适量减少。"
                    + "可配合叶面喷施硼肥和钙肥，提高坐果率和果实品质。每 7-10 天追肥一次。";
        }
        return "【Mock 回答】这是模拟的 AI 回答。在 Mock 模式下，系统不调用真实 DeepSeek API。"
                + "您的问题涉及农业知识，建议参考知识库文档获取更详细的信息。"
                + "真实环境下将基于 Chroma 向量检索结果生成精准回答。";
    }

    @Override
    public String getEngineName() {
        return "mock-llm";
    }
}
