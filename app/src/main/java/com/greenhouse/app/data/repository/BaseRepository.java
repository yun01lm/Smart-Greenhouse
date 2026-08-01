package com.greenhouse.app.data.repository;

import android.os.Handler;
import java.util.LinkedHashMap;
import java.util.Map;

import android.os.Looper;

import com.greenhouse.app.data.api.ApiClient;
import com.greenhouse.app.data.api.GreenhouseApiService;

import java.util.concurrent.ExecutorService;

/**
 * Repository 基类
 * <p>
 * 封装所有 Repository 共用的基础设施：
 * API 服务引用、后台线程池、主线程 Handler、统一错误处理。
 * 所有具体 Repository 继承此类，避免在每个类中重复这些逻辑。
 * </p>
 */
public abstract class BaseRepository {

    protected final GreenhouseApiService apiService;
    protected final ExecutorService executor;
    protected final Handler mainHandler;
    /** LRU 内存缓存（最大 50 条，30 秒过期） */
    private static final Map<String, CacheEntry<?>> cache = new LinkedHashMap<String, CacheEntry<?>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<?>> eldest) {
            return size() > 50;
        }
    };
    private static final long CACHE_TTL_MS = 30_000;

    private static class CacheEntry<T> {
        final T data;
        final long timestamp;
        CacheEntry(T data) { this.data = data; this.timestamp = System.currentTimeMillis(); }
        boolean isExpired() { return System.currentTimeMillis() - timestamp > CACHE_TTL_MS; }
    }


    public BaseRepository() {
        this.apiService = ApiClient.getApiService();
        this.executor = ApiClient.getExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 通用回调接口
     */
    public interface Callback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    /**
     * 在后台线程执行任务
     */
    
    /** 从缓存获取 */
    @SuppressWarnings("unchecked")
    protected synchronized <T> T getFromCache(String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) return (T) entry.data;
        cache.remove(key);
        return null;
    }

    /** 写入缓存 */
    protected synchronized void putToCache(String key, Object data) {
        cache.put(key, new CacheEntry<>(data));
    }

    /** 清除缓存 */
    protected synchronized void clearCache() { cache.clear(); }
    protected void execute(Runnable task) {
        executor.execute(task);
    }

    /**
     * 将成功结果回传主线程
     */
    protected <T> void postSuccess(Callback<T> callback, T data) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(data));
        }
    }

    /**
     * 将错误消息回传主线程
     */
    protected <T> void postError(Callback<T> callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }

    /**
     * 从 Retrofit Response 中解析错误信息
     */
    protected String parseError(retrofit2.Response<?> response) {
        if (response.body() instanceof com.greenhouse.app.data.model.ApiResponse) {
            com.greenhouse.app.data.model.ApiResponse<?> apiResp =
                    (com.greenhouse.app.data.model.ApiResponse<?>) response.body();
            if (apiResp != null && apiResp.getMessage() != null) {
                return apiResp.getMessage();
            }
        }
        return "请求失败 (HTTP " + response.code() + ")";
    }
}
