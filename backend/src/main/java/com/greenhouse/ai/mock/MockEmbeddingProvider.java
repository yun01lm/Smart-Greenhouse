package com.greenhouse.ai.mock;

import com.greenhouse.ai.EmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock Embedding Provider
 * <p>
 * 验收时使用，生成随机但维度正确的向量，不调用 SiliconFlow API。
 * 同一文本多次调用返回相同向量（基于 hashCode 种子）。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.embedding.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingProvider implements EmbeddingProvider {

    private static final int DIMENSION = 1024; // bge-m3 维度

    @Override
    public float[] embed(String text) throws Exception {
        log.info("[MOCK] Embedding: textLength={}", text.length());
        return generateVector(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) throws Exception {
        log.info("[MOCK] Embedding batch: count={}", texts.size());
        List<float[]> result = new ArrayList<>();
        for (String text : texts) {
            result.add(generateVector(text));
        }
        return result;
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }

    @Override
    public String getEngineName() {
        return "mock-embedding";
    }

    /**
     * 基于文本 hashCode 生成确定性的模拟向量
     */
    private float[] generateVector(String text) {
        Random rng = new Random(text.hashCode());
        float[] vec = new float[DIMENSION];
        double norm = 0;
        for (int i = 0; i < DIMENSION; i++) {
            vec[i] = (float) (rng.nextGaussian() * 0.1);
            norm += vec[i] * vec[i];
        }
        // L2 归一化
        norm = Math.sqrt(norm);
        for (int i = 0; i < DIMENSION; i++) {
            vec[i] = (float) (vec[i] / norm);
        }
        return vec;
    }
}
