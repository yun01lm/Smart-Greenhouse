package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.HistoryDataPoint;
import com.greenhouse.app.data.model.HistoryResponse;
import com.greenhouse.app.data.repository.GreenhouseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 历史数据 ViewModel
 * <p>
 * 管理传感器类型选择、时间范围切换、历史数据加载。
 * 符合规范：ViewModel 不持有 Context。
 * </p>
 */
public class HistoryViewModel extends ViewModel {

    private final GreenhouseRepository repository;

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
        this.repository = new GreenhouseRepository();
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
        list.add(new SensorTypeItem("TEMP", "空气温度", "°C"));
        list.add(new SensorTypeItem("HUMIDITY", "空气湿度", "%"));
        list.add(new SensorTypeItem("LIGHT", "光照强度", "lux"));
        list.add(new SensorTypeItem("CO2", "CO₂浓度", "ppm"));
        list.add(new SensorTypeItem("O2", "O₂浓度", "%"));
        list.add(new SensorTypeItem("SOIL_TEMP", "土壤温度", "°C"));
        list.add(new SensorTypeItem("SOIL_HUMIDITY", "土壤湿度", "%"));
        list.add(new SensorTypeItem("EC", "土壤EC值", "mS/cm"));
        list.add(new SensorTypeItem("N", "氮(N)", "mg/kg"));
        list.add(new SensorTypeItem("P", "磷(P)", "mg/kg"));
        list.add(new SensorTypeItem("K", "钾(K)", "mg/kg"));
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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        String startTime = sdf.format(start.getTime());
        String endTime = sdf.format(end.getTime());

        repository.getHistory(currentGreenhouseId, sensorType, startTime, endTime, aggregation,
                new GreenhouseRepository.Callback<HistoryResponse>() {
                    @Override
                    public void onSuccess(HistoryResponse data) {
                        isLoading.postValue(false);
                        responseMeta.postValue(data);
                        if (data.getDataPoints() != null) {
                            dataPoints.postValue(data.getDataPoints());
                        } else {
                            dataPoints.postValue(new ArrayList<>());
                        }
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
