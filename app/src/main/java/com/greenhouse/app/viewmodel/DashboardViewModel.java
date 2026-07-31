package com.greenhouse.app.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.greenhouse.app.data.model.Greenhouse;
import com.greenhouse.app.data.model.HealthScoreData;
import com.greenhouse.app.data.model.SensorDataPoint;
import com.greenhouse.app.data.model.SensorRealtimeData;
import com.greenhouse.app.data.repository.SensorRepository;
import com.greenhouse.app.data.repository.HealthRepository;
import com.greenhouse.app.websocket.StompClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实时数据看板 ViewModel
 * <p>
 * 管理大棚列表、传感器实时数据、WebSocket 订阅。
 * 符合规范：业务逻辑全部在 ViewModel 中，Activity 只负责 UI。
 * </p>
 */
public class DashboardViewModel extends ViewModel {

    private static final String TAG = "DashboardVM";

    private final SensorRepository sensorRepo;
    private final HealthRepository healthRepo;
    private final Gson gson;
    private StompClient stompClient;

    // 大棚列表
    private final MutableLiveData<List<Greenhouse>> greenhouses = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Long> selectedGreenhouseId = new MutableLiveData<>();

    // 传感器实时数据（按 sensorType -> latest value）
    private final MutableLiveData<Map<String, Double>> sensorData = new MutableLiveData<>(new HashMap<>());
    // 原始数据点列表
    private final MutableLiveData<List<SensorDataPoint>> dataPoints = new MutableLiveData<>(new ArrayList<>());

    // 健康评分
    private final MutableLiveData<HealthScoreData> healthScore = new MutableLiveData<>();

    // 加载状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // WebSocket 连接状态
    private final MutableLiveData<Boolean> wsConnected = new MutableLiveData<>(false);

    public DashboardViewModel() {
        this.sensorRepo = new SensorRepository();
        this.healthRepo = new HealthRepository();
        this.gson = new Gson();
    }

    // ===== Getters =====

    public LiveData<List<Greenhouse>> getGreenhouses() { return greenhouses; }
    public LiveData<Long> getSelectedGreenhouseId() { return selectedGreenhouseId; }
    public LiveData<Map<String, Double>> getSensorData() { return sensorData; }
    public LiveData<List<SensorDataPoint>> getDataPoints() { return dataPoints; }
    public LiveData<HealthScoreData> getHealthScore() { return healthScore; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getWsConnected() { return wsConnected; }

    // ===== 大棚操作 =====

    /** 加载大棚列表 */
    public void loadGreenhouses() {
        isLoading.setValue(true);
        sensorRepo.getGreenhouses(new SensorRepository.Callback<List<Greenhouse>>() {
            @Override
            public void onSuccess(List<Greenhouse> data) {
                greenhouses.postValue(data);
                isLoading.postValue(false);
                // 默认选中第一个
                if (data != null && !data.isEmpty() && selectedGreenhouseId.getValue() == null) {
                    selectGreenhouse(data.get(0).getId());
                }
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    /** 切换大棚 */
    public void selectGreenhouse(long greenhouseId) {
        selectedGreenhouseId.setValue(greenhouseId);
        loadRealtimeData(greenhouseId);
        loadHealthScore(greenhouseId);
        subscribeWebSocket(greenhouseId);
    }

    // ===== 数据加载 =====

    private void loadRealtimeData(long greenhouseId) {
        sensorRepo.getRealtimeData(greenhouseId, new SensorRepository.Callback<SensorRealtimeData>() {
            @Override
            public void onSuccess(SensorRealtimeData data) {
                // 转换：从 dataByType Map 提取每个传感器类型的最新值
                Map<String, Double> latestValues = new HashMap<>();
                List<SensorDataPoint> allPoints = new ArrayList<>();

                if (data.getDataByType() != null) {
                    for (Map.Entry<String, List<SensorDataPoint>> entry : data.getDataByType().entrySet()) {
                        List<SensorDataPoint> points = entry.getValue();
                        if (points != null && !points.isEmpty()) {
                            latestValues.put(entry.getKey(), points.get(0).getValue());
                            allPoints.addAll(points);
                        }
                    }
                }

                sensorData.postValue(latestValues);
                dataPoints.postValue(allPoints);
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "加载实时数据失败: " + message);
            }
        });
    }

    private void loadHealthScore(long greenhouseId) {
        healthRepo.getHealthScore(greenhouseId, new SensorRepository.Callback<HealthScoreData>() {
            @Override
            public void onSuccess(HealthScoreData data) {
                healthScore.postValue(data);
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "加载健康评分失败: " + message);
            }
        });
    }

    // ===== WebSocket =====

    private void subscribeWebSocket(long greenhouseId) {
        // 断开旧连接
        if (stompClient != null) {
            stompClient.unsubscribe("/topic/greenhouse/" + greenhouseId + "/realtime");
        }

        // TODO: 从 TokenManager 获取 token 建立 STOMP 连接
        // stompClient.subscribe("/topic/greenhouse/" + greenhouseId + "/realtime", message -> {
        //     handleRealtimeMessage(message);
        // });
    }

    /** 处理 WebSocket 推送的实时数据 */
    private void handleRealtimeMessage(String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            String type = json.get("type").getAsString();

            if ("SENSOR_DATA".equals(type)) {
                String sensorType = json.get("sensorType").getAsString();
                double value = json.get("value").getAsDouble();

                Map<String, Double> current = sensorData.getValue();
                if (current != null) {
                    current = new HashMap<>(current);
                    current.put(sensorType, value);
                    sensorData.postValue(current);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "解析 WebSocket 消息失败: " + e.getMessage());
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (stompClient != null) {
            stompClient.disconnect();
        }
    }
}
