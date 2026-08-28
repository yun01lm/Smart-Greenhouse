package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.HistoryDataPoint;
import com.greenhouse.app.data.model.HistoryResponse;
import com.greenhouse.app.data.model.SensorDataPoint;
import com.greenhouse.app.data.repository.SensorRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 历史数据 ViewModel
 * <p>
 * 管理传感器类型选择、时间范围切换、历史数据加载。
 * 符合规范：ViewModel 不持有 Context。
 * </p>
 */
public class HistoryViewModel extends ViewModel {

    private final SensorRepository repository;

    // 传感器类型列表（11种）
    private final MutableLiveData<List<SensorTypeItem>> sensorTypes = new MutableLiveData<>();
    // 选中的传感器类型
    private final MutableLiveData<String> selectedSensorType = new MutableLiveData<>("TEMP");
    // 时间范围标签
    private final MutableLiveData<String> selectedTimeRange = new MutableLiveData<>("24h");
    // 历史数据点
    private final MutableLiveData<List<HistoryDataPoint>> dataPoints = new MutableLiveData<>(new ArrayList<>());
    // 响应元数据
    private final MutableLiveData<HistoryResponse> responseMeta = new MutableLiveData<>();
    // 加载状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // 错误信息
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long currentGreenhouseId;

    public HistoryViewModel() {
        this.repository = new SensorRepository();
        initSensorTypes();
    }

    // ===== LiveData =====

    public LiveData<List<SensorTypeItem>> getSensorTypes() { return sensorTypes; }
    public LiveData<String> getSelectedSensorType() { return selectedSensorType; }
    public LiveData<String> getSelectedTimeRange() { return selectedTimeRange; }
    public LiveData<List<HistoryDataPoint>> getDataPoints() { return dataPoints; }
    public LiveData<HistoryResponse> getResponseMeta() { return responseMeta; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void setCurrentGreenhouseId(long id) { this.currentGreenhouseId = id; }

    // ===== 传感器类型 =====

    private void initSensorTypes() {
        List<SensorTypeItem> list = new ArrayList<>();
        // 类型代码与后端 SensorType 枚举严格一致（TEMPERATURE/HUMIDITY/LIGHT/CO2/SOIL_TEMP/SOIL_MOISTURE/SOIL_PH/WIND_SPEED）
        list.add(new SensorTypeItem("TEMPERATURE", "空气温度", "°C"));
        list.add(new SensorTypeItem("HUMIDITY", "空气湿度", "%"));
        list.add(new SensorTypeItem("LIGHT", "光照强度", "lux"));
        list.add(new SensorTypeItem("CO2", "CO₂浓度", "ppm"));
        list.add(new SensorTypeItem("SOIL_TEMP", "土壤温度", "°C"));
        list.add(new SensorTypeItem("SOIL_MOISTURE", "土壤湿度", "%"));
        list.add(new SensorTypeItem("SOIL_PH", "土壤pH", "pH"));
        list.add(new SensorTypeItem("WIND_SPEED", "风速", "m/s"));
        sensorTypes.setValue(list);
    }

    // ===== 选择操作 =====

    public void selectSensorType(String type) {
        selectedSensorType.setValue(type);
        loadHistory();
    }

    public void selectTimeRange(String range) {
        selectedTimeRange.setValue(range);
        loadHistory();
    }

    // ===== 加载历史数据 =====

    public void loadHistory() {
        String sensorType = selectedSensorType.getValue();
        String timeRange = selectedTimeRange.getValue();
        if (sensorType == null || timeRange == null) return;

        isLoading.setValue(true);

        // 计算时间范围
        Calendar end = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        String aggregation;

        switch (timeRange) {
            case "1h":
                start.add(Calendar.HOUR_OF_DAY, -1);
                aggregation = "1m";
                break;
            case "24h":
                start.add(Calendar.HOUR_OF_DAY, -24);
                aggregation = "30m";
                break;
            case "7d":
                start.add(Calendar.DAY_OF_YEAR, -7);
                aggregation = "6h";
                break;
            case "30d":
                start.add(Calendar.DAY_OF_YEAR, -30);
                aggregation = "1d";
                break;
            default:
                start.add(Calendar.HOUR_OF_DAY, -24);
                aggregation = "30m";
        }

        long startMillis = start.getTimeInMillis();
        long endMillis = end.getTimeInMillis();

        repository.getHistory(currentGreenhouseId, sensorType, startMillis, endMillis, aggregation,
                new SensorRepository.Callback<List<SensorDataPoint>>() {
                    @Override
                    public void onSuccess(List<SensorDataPoint> data) {
                        isLoading.postValue(false);
                        List<HistoryDataPoint> points = new ArrayList<>();
                        if (data != null) {
                            for (SensorDataPoint p : data) {
                                HistoryDataPoint dp = new HistoryDataPoint();
                                String ts = p.getTimestamp();
                                if (ts != null) {
                                    // 后端 ISO-8601（可能带毫秒/Z），统一截取到秒
                                    if (ts.length() > 19) ts = ts.substring(0, 19);
                                    dp.setTime(ts);
                                }
                                dp.setAvg(p.getValue());
                                points.add(dp);
                            }
                        }
                        HistoryResponse meta = new HistoryResponse();
                        meta.setSensorType(sensorType);
                        meta.setDataPoints(points);
                        responseMeta.postValue(meta);
                        dataPoints.postValue(points);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("加载历史数据失败: " + message);
                    }
                });
    }

    // ===== 传感器类型项 =====

    public static class SensorTypeItem {
        private final String code;
        private final String name;
        private final String unit;

        public SensorTypeItem(String code, String name, String unit) {
            this.code = code;
            this.name = name;
            this.unit = unit;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getUnit() { return unit; }

        @Override
        public String toString() {
            return name + " (" + unit + ")";
        }
    }
}
