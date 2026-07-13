package com.greenhouse.app.data.repository;

import android.os.Handler;
import android.os.Looper;

import com.greenhouse.app.data.api.ApiClient;
import com.greenhouse.app.data.api.GreenhouseApiService;
import com.greenhouse.app.data.model.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import retrofit2.Response;

/**
 * 数据仓库
 * <p>
 * 封装所有 API 调用，在后台线程执行网络请求（ExecutorService），
 * 通过 Handler 回调主线程更新 UI。
 * 符合规范：网络请求在子线程执行，禁止主线程IO。
 * </p>
 */
public class GreenhouseRepository {

    private final GreenhouseApiService apiService;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public GreenhouseRepository() {
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

    // ===== 认证 =====

    public void login(String username, String password, Callback<LoginResponse> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<LoginResponse>> response =
                        apiService.login(new LoginRequest(username, password)).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    // ===== 大棚 =====

    public void getGreenhouses(Callback<List<Greenhouse>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<Greenhouse>>> response =
                        apiService.getGreenhouses().execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    // ===== 实时数据 =====

    public void getRealtimeData(long greenhouseId, Callback<SensorRealtimeData> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<SensorRealtimeData>> response =
                        apiService.getRealtimeData(greenhouseId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    // ===== 预警 =====

    public void getAlerts(long greenhouseId, int page, int size, Callback<PageResult<AlertItem>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<PageResult<AlertItem>>> response =
                        apiService.getAlerts(greenhouseId, page, size).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    // ===== 健康评分 =====

    public void getHealthScore(long greenhouseId, Callback<HealthScoreData> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<HealthScoreData>> response =
                        apiService.getHealthScore(greenhouseId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    // ===== 辅助方法 =====

    private void execute(Runnable task) {
        executor.execute(task);
    }

    private <T> void postSuccess(Callback<T> callback, T data) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(data));
        }
    }

    private <T> void postError(Callback<T> callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }

    private String parseError(retrofit2.Response<?> response) {
        if (response.body() instanceof ApiResponse) {
            ApiResponse<?> apiResp = (ApiResponse<?>) response.body();
            if (apiResp != null && apiResp.getMessage() != null) {
                return apiResp.getMessage();
            }
        }
        return "请求失败 (HTTP " + response.code() + ")";
    }
}
