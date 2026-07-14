package com.greenhouse.module.qa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Chroma 向量检索服务
 * <p>
 * 通过 HTTP REST API 调用 Chroma 向量数据库进行相似度检索。
 * 使用 collection "greenhouse_knowledge" 存储农业知识文档片段。
 * </p>
 *
 * <h3>Chroma REST API</h3>
 * <ul>
 *   <li>查询集合：POST /api/v1/collections/{name}/query</li>
 * </ul>
 */
@Slf4j
@Service
public class ChromaRetrievalService {

    private final String chromaUrl;
    private final String collectionName;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public ChromaRetrievalService(
            @Value("${chroma.base-url:http://localhost:8000}") String chromaUrl,
            @Value("${chroma.collection:greenhouse_knowledge}") String collectionName,
            ObjectMapper objectMapper) {
        this.chromaUrl = chromaUrl;
        this.collectionName = collectionName;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 向量相似度检索
     *
     * @param queryEmbedding 查询向量（1024 维）
     * @param topK           返回 Top-K 结果
     * @return 检索结果列表，每条包含文档内容、元数据和相似度
     */
    public List<RetrievalResult> query(List<Double> queryEmbedding, int topK) {
        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();

            // query_embeddings: [ [vector] ]
            ArrayNode embeddings = objectMapper.createArrayNode();
            ArrayNode vector = objectMapper.createArrayNode();
            for (Double v : queryEmbedding) {
                vector.add(v);
            }
            embeddings.add(vector);
            requestBody.set("query_embeddings", embeddings);

            requestBody.put("n_results", topK);

            // include: ["documents", "metadatas", "distances"]
            ArrayNode include = objectMapper.createArrayNode();
            include.add("documents");
            include.add("metadatas");
            include.add("distances");
            requestBody.set("include", include);

            String url = chromaUrl + "/api/v1/collections/" + collectionName + "/query";

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(
                            objectMapper.writeValueAsString(requestBody),
                            MediaType.parse("application/json")))
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Chroma 检索失败: HTTP {} body={}", response.code(), errorBody);
                    // Chroma 未启动或集合不存在时返回空结果，不阻塞问答
                    return Collections.emptyList();
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                return parseQueryResponse(responseBody);
            }
        } catch (IOException e) {
            log.error("Chroma 连接失败: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Chroma 解析失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 解析 Chroma 查询响应
     */
    private List<RetrievalResult> parseQueryResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // 响应格式：
        // {
        //   "ids": [["id1", "id2", ...]],
        //   "documents": [["doc1", "doc2", ...]],
        //   "metadatas": [[{"title":"...","category":"..."}, ...]],
        //   "distances": [[0.12, 0.25, ...]]
        // }

        JsonNode ids = root.get("ids");
        JsonNode documents = root.get("documents");
        JsonNode metadatas = root.get("metadatas");
        JsonNode distances = root.get("distances");

        // Chroma 返回的是二维数组：[[item1, item2, ...]]
        // 第一维对应 query_embeddings 中的每个查询向量
        if (ids == null || !ids.isArray() || ids.isEmpty()) return Collections.emptyList();
        if (documents == null || !documents.isArray() || documents.isEmpty()) return Collections.emptyList();

        JsonNode idsArr = ids.get(0);
        JsonNode docsArr = documents.get(0);
        JsonNode metasArr = (metadatas != null && metadatas.isArray() && !metadatas.isEmpty())
                ? metadatas.get(0) : null;
        JsonNode distsArr = (distances != null && distances.isArray() && !distances.isEmpty())
                ? distances.get(0) : null;

        List<RetrievalResult> results = new ArrayList<>();
        for (int i = 0; i < docsArr.size(); i++) {
            String docContent = docsArr.get(i).asText();

            String title = "";
            String category = "";
            if (metasArr != null && i < metasArr.size() && metasArr.get(i) != null) {
                JsonNode meta = metasArr.get(i);
                if (meta.has("title")) title = meta.get("title").asText();
                if (meta.has("category")) category = meta.get("category").asText();
            }

            double distance = 0.0;
            if (distsArr != null && i < distsArr.size()) {
                distance = distsArr.get(i).asDouble();
            }
            // 距离转相似度：Chroma 默认用 L2 距离，相似度 = 1/(1+distance)
            double similarity = 1.0 / (1.0 + distance);

            results.add(new RetrievalResult(docContent, title, category, similarity));
        }

        log.debug("Chroma 检索完成: topK={}, actual_results={}", docsArr.size(), results.size());
        return results;
    }

    /**
     * 检索结果
     */
    public record RetrievalResult(
            /** 文档内容 */
            String content,
            /** 文档标题 */
            String title,
            /** 文档分类 */
            String category,
            /** 相似度 0.0-1.0 */
            double similarity
    ) {}
}
