package com.greenhouse.module.qa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ChromaDB 初始化服务
 * <p>
 * 应用启动时自动检查并创建 ChromaDB collection，缓存其 UUID。
 * 解决 Chroma v2 API 需要通过 UUID（而非名称）访问 collection 的问题。
 * </p>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>检查 collection 是否存在（GET /collections）</li>
 *   <li>如不存在，创建 collection（POST /collections）</li>
 *   <li>缓存 collection UUID，供所有服务使用</li>
 * </ol>
 */
@Slf4j
@Component
public class ChromaInitializer {

    private final String chromaUrl;
    private final String collectionName;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    /** 缓存的 collection UUID */
    private final AtomicReference<String> collectionId = new AtomicReference<>(null);

    public ChromaInitializer(
            @Value("${chroma.base-url:http://localhost:8000}") String chromaUrl,
            @Value("${chroma.collection:greenhouse_knowledge}") String collectionName,
            ObjectMapper objectMapper) {
        this.chromaUrl = chromaUrl;
        this.collectionName = collectionName;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 应用启动后自动初始化 Chroma collection
     */
    @PostConstruct
    public void initialize() {
        try {
            // 等待 ChromaDB 启动（最多重试 5 次，每次间隔 3 秒）
            String id = getOrCreateCollection(5);
            if (id != null) {
                collectionId.set(id);
                log.info("ChromaDB 初始化完成: collection={}, uuid={}", collectionName, id);
            } else {
                log.error("ChromaDB 初始化失败: 无法创建或获取 collection '{}'", collectionName);
            }
        } catch (Exception e) {
            log.error("ChromaDB 初始化异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取缓存的 collection UUID
     *
     * @return collection UUID，如果未初始化返回 null
     */
    public String getCollectionId() {
        return collectionId.get();
    }

    /**
     * 构建 v2 API 基础路径（使用 UUID）
     *
     * @return /api/v2/tenants/default/databases/default/collections/{uuid}
     */
    public String getCollectionPath() {
        String id = collectionId.get();
        if (id == null) {
            throw new IllegalStateException("ChromaDB collection 尚未初始化");
        }
        return "/api/v2/tenants/default/databases/default/collections/" + id;
    }

    /**
     * 获取或创建 collection
     *
     * @param maxRetries 最大重试次数
     * @return collection UUID，失败返回 null
     */
    private String getOrCreateCollection(int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 0. 确保 database 存在（ChromaDB 不会自动创建 default database）
                ensureDatabase();

                // 1. 检查是否已存在
                String existingId = findCollection(collectionName);
                if (existingId != null) {
                    log.info("ChromaDB collection 已存在: {} (uuid={})", collectionName, existingId);
                    return existingId;
                }

                // 2. 不存在则创建
                log.info("ChromaDB collection '{}' 不存在，正在创建...", collectionName);
                String newId = createCollection(collectionName);
                if (newId != null) {
                    log.info("ChromaDB collection 已创建: {} (uuid={})", collectionName, newId);
                    return newId;
                }

                log.warn("创建 collection 失败 (attempt {}/{})，{}秒后重试...",
                        attempt, maxRetries, 3);
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                log.warn("ChromaDB 连接失败 (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 确保 default database 存在（ChromaDB 不会自动创建）
     */
    private void ensureDatabase() throws IOException {
        String url = chromaUrl + "/api/v2/tenants/default/databases";
        Request getRequest = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(getRequest).execute()) {
            if (!response.isSuccessful()) return;
            String body = response.body() != null ? response.body().string() : "[]";
            JsonNode root = objectMapper.readTree(body);

            boolean hasDefault = false;
            if (root.isArray()) {
                for (JsonNode db : root) {
                    if ("default".equals(db.get("name").asText())) {
                        hasDefault = true;
                        break;
                    }
                }
            }

            if (!hasDefault) {
                log.info("ChromaDB default database 不存在，正在创建...");
                ObjectNode dbBody = objectMapper.createObjectNode();
                dbBody.put("name", "default");
                Request createReq = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(
                                objectMapper.writeValueAsString(dbBody),
                                MediaType.parse("application/json")))
                        .addHeader("Content-Type", "application/json")
                        .build();
                try (Response createResp = httpClient.newCall(createReq).execute()) {
                    if (createResp.isSuccessful()) {
                        log.info("ChromaDB default database 已创建");
                    } else {
                        log.warn("ChromaDB default database 创建失败: HTTP {}",
                                createResp.code());
                    }
                }
            }
        }
    }

    /**
     * 查找 collection 并返回 UUID
     */
    private String findCollection(String name) throws IOException {
        String url = chromaUrl + "/api/v2/tenants/default/databases/default/collections";
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("ChromaDB 查询 collections 失败: HTTP {}", response.code());
                return null;
            }
            String body = response.body() != null ? response.body().string() : "[]";
            JsonNode root = objectMapper.readTree(body);

            if (root.isArray()) {
                for (JsonNode col : root) {
                    if (col.has("name") && name.equals(col.get("name").asText())
                            && col.has("id")) {
                        return col.get("id").asText();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 创建 collection 并返回 UUID
     */
    private String createCollection(String name) throws IOException {
        String url = chromaUrl + "/api/v2/tenants/default/databases/default/collections";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", name);
        // metadata 可选，添加描述信息
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("description", "智慧大棚AIoT系统知识库");
        body.set("metadata", metadata);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(
                        objectMapper.writeValueAsString(body),
                        MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String respBody = response.body() != null ? response.body().string() : "{}";
                JsonNode root = objectMapper.readTree(respBody);
                if (root.has("id")) {
                    return root.get("id").asText();
                }
                // 某些版本返回 name 而非 id，重新查找
                return findCollection(name);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("ChromaDB 创建 collection 失败: HTTP {} body={}", response.code(), errorBody);
                return null;
            }
        }
    }
}
