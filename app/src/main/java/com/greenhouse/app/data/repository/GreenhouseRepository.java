package com.greenhouse.app.data.repository;

import android.os.Handler;
import android.os.Looper;

import com.greenhouse.app.data.api.ApiClient;
import com.greenhouse.app.data.api.GreenhouseApiService;
import com.greenhouse.app.data.model.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
        getAlerts(greenhouseId, page, size, null, callback);
    }

    public void getAlerts(long greenhouseId, int page, int size, String level,
                          Callback<PageResult<AlertItem>> callback) {
        execute(() -> {
            try {
                retrofit2.Response<ApiResponse<PageResult<AlertItem>>> response;
                if (level != null && !level.isEmpty()) {
                    response = apiService.getAlertsByLevel(greenhouseId, page, size, level).execute();
                } else {
                    response = apiService.getAlerts(greenhouseId, page, size).execute();
                }
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

    public void markAlertRead(long alertId, Callback<Void> callback) {
        execute(() -> {
            try {
                retrofit2.Response<ApiResponse<Void>> response =
                        apiService.markAlertRead(alertId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, null);
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    // ===== 自定义阈值 =====

    public void getThresholds(long greenhouseId, Callback<List<ThresholdItem>> callback) {
        execute(() -> {
            try {
                retrofit2.Response<ApiResponse<List<ThresholdItem>>> response =
                        apiService.getThresholds(greenhouseId).execute();
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

    public void setThreshold(ThresholdItem threshold, Callback<ThresholdItem> callback) {
        execute(() -> {
            try {
                retrofit2.Response<ApiResponse<ThresholdItem>> response =
                        apiService.setThreshold(threshold).execute();
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

    /**
     * 获取健康评分详情/报告
     */
    public void getHealthDetail(long id, Callback<HealthScoreData> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<HealthScoreData>> response =
                        apiService.getHealthDetail(id).execute();
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

    /**
     * 获取健康评分历史
     */
    public void getHealthHistory(long greenhouseId, int page, int size,
                                  Callback<PageResult<HealthScoreData>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<PageResult<HealthScoreData>>> response =
                        apiService.getHealthHistory(greenhouseId, page, size).execute();
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

    // ===== C8 病虫害诊断 =====

    /**
     * 上传图片进行病虫害诊断
     * @param imageFile     图片文件
     * @param greenhouseId  大棚ID
     */
    public void diagnose(File imageFile, long greenhouseId, Callback<DiagnosisResponse> callback) {
        execute(() -> {
            try {
                RequestBody requestFile = RequestBody.create(
                        MediaType.parse("image/jpeg"), imageFile);
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                        "image", imageFile.getName(), requestFile);
                RequestBody greenhouseIdBody = RequestBody.create(
                        MediaType.parse("text/plain"), String.valueOf(greenhouseId));

                Response<ApiResponse<DiagnosisResponse>> response =
                        apiService.diagnose(imagePart, greenhouseIdBody).execute();
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

    /**
     * 获取诊断历史记录
     * @param greenhouseId  大棚ID
     * @param page          页码（从1开始）
     * @param size          每页条数
     */
    public void getDiagnosisHistory(long greenhouseId, int page, int size,
                                     Callback<PageResult<DiagnosisHistoryItem>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<PageResult<DiagnosisHistoryItem>>> response =
                        apiService.getDiagnosisHistory(greenhouseId, page, size).execute();
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

    // ===== C9 AI问答 =====

    public void ask(String question, long greenhouseId, Callback<QaResponse> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<QaResponse>> response =
                        apiService.ask(new QaRequest(question, greenhouseId)).execute();
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

    public void askVoice(File audioFile, long greenhouseId, Callback<QaResponse> callback) {
        execute(() -> {
            try {
                RequestBody requestFile = RequestBody.create(
                        MediaType.parse("audio/aac"), audioFile);
                MultipartBody.Part audioPart = MultipartBody.Part.createFormData(
                        "audio", audioFile.getName(), requestFile);
                RequestBody greenhouseIdBody = RequestBody.create(
                        MediaType.parse("text/plain"), String.valueOf(greenhouseId));
                Response<ApiResponse<QaResponse>> response =
                        apiService.askVoice(audioPart, greenhouseIdBody).execute();
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

    public void getQaHistory(int page, int size, Callback<PageResult<QaHistoryItem>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<PageResult<QaHistoryItem>>> response =
                        apiService.getQaHistory(page, size).execute();
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

    // ===== C7 设备控制 =====

    public void getActuators(long greenhouseId, Callback<List<ActuatorInfo>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<ActuatorInfo>>> response =
                        apiService.getActuators(greenhouseId).execute();
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

    public void controlActuator(long actuatorId, String action, long greenhouseId,
                                Callback<ControlResponse> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<ControlResponse>> response =
                        apiService.controlActuator(new ControlRequest(actuatorId, action, greenhouseId)).execute();
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

    public void getScenes(long greenhouseId, Callback<List<SceneInfo>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<SceneInfo>>> response =
                        apiService.getScenes(greenhouseId).execute();
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

    public void executeScene(long sceneId, long greenhouseId,
                             Callback<ControlResponse> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<ControlResponse>> response =
                        apiService.executeScene(sceneId, new SceneExecuteRequest(greenhouseId)).execute();
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

    // ===== C5 历史数据 =====

    public void getHistory(long greenhouseId, String sensorType,
                           String startTime, String endTime, String aggregation,
                           Callback<HistoryResponse> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<HistoryResponse>> response =
                        apiService.getHistory(greenhouseId, sensorType, startTime, endTime, aggregation).execute();
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

    // ===== F7 长势评估 =====

    /**
     * 获取最新长势评估
     */
    public void getLatestGrowthAssessment(long greenhouseId, Callback<GrowthAssessment> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<GrowthAssessment>> response =
                        apiService.getLatestGrowthAssessment(greenhouseId).execute();
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

    /**
     * 获取长势评估历史记录
     */
    public void getGrowthHistory(long greenhouseId, int page, int size,
                                  Callback<PageResult<GrowthAssessment>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<PageResult<GrowthAssessment>>> response =
                        apiService.getGrowthHistory(greenhouseId, page, size).execute();
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

    /**
     * 获取截帧图片列表
     */
    public void getGrowthImages(long greenhouseId, String date, int page, int size,
                                 Callback<PageResult<GrowthImage>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<PageResult<GrowthImage>>> response =
                        apiService.getGrowthImages(greenhouseId, date, page, size).execute();
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

    // ===== C22 作物生长周期 =====

    /**
     * 获取种植周期列表
     */
    public void getCropCycles(long greenhouseId, Callback<List<CropCycleData>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<CropCycleData>>> response =
                        apiService.getCropCycles(greenhouseId).execute();
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
