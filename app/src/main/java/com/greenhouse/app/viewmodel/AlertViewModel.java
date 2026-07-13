package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.AlertItem;
import com.greenhouse.app.data.model.PageResult;
import com.greenhouse.app.data.model.ThresholdItem;
import com.greenhouse.app.data.repository.GreenhouseRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 环境预警 ViewModel (F02)
 * <p>
 * 管理预警列表加载、筛选、已读标记、自定义阈值。
 * 符合规范：业务逻辑全部在 ViewModel。
 * </p>
 */
public class AlertViewModel extends ViewModel {

    private final GreenhouseRepository repository;

    // 预警列表
    private final MutableLiveData<List<AlertItem>> alerts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);

    // 筛选条件
    private final MutableLiveData<String> filterLevel = new MutableLiveData<>(null);
    private final MutableLiveData<Long> greenhouseId = new MutableLiveData<>();

    // 阈值
    private final MutableLiveData<List<ThresholdItem>> thresholds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> thresholdMessage = new MutableLiveData<>();

    // 状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    public AlertViewModel() {
        this.repository = new GreenhouseRepository();
    }

    public LiveData<List<AlertItem>> getAlerts() { return alerts; }
    public LiveData<Boolean> getHasMore() { return hasMore; }
    public LiveData<String> getFilterLevel() { return filterLevel; }
    public LiveData<List<ThresholdItem>> getThresholds() { return thresholds; }
    public LiveData<String> getThresholdMessage() { return thresholdMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /** 设置大棚ID并加载预警 */
    public void init(long ghId) {
        this.greenhouseId.setValue(ghId);
        loadAlerts(true);
        loadThresholds();
    }

    /** 加载预警列表 */
    public void loadAlerts(boolean refresh) {
        Long ghId = greenhouseId.getValue();
        if (ghId == null) return;

        if (refresh) {
            currentPage = 0;
            alerts.setValue(new ArrayList<>());
        }

        isLoading.setValue(true);

        String level = filterLevel.getValue();
        repository.getAlerts(ghId, currentPage, PAGE_SIZE, level,
                new GreenhouseRepository.Callback<PageResult<AlertItem>>() {
            @Override
            public void onSuccess(PageResult<AlertItem> data) {
                List<AlertItem> current = alerts.getValue();
                if (current == null) current = new ArrayList<>();

                if (refresh) {
                    current = new ArrayList<>(data.getList());
                } else {
                    current.addAll(data.getList());
                }

                alerts.postValue(current);
                hasMore.postValue(current.size() < data.getTotal());
                isLoading.postValue(false);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    /** 加载更多 */
    public void loadMore() {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        if (Boolean.FALSE.equals(hasMore.getValue())) return;
        currentPage++;
        loadAlerts(false);
    }

    /** 按级别筛选 */
    public void filterByLevel(String level) {
        filterLevel.setValue(level);
        loadAlerts(true);
    }

    /** 清除筛选 */
    public void clearFilter() {
        filterLevel.setValue(null);
        loadAlerts(true);
    }

    /** 标记已读 */
    public void markRead(long alertId) {
        repository.markAlertRead(alertId, new GreenhouseRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // 更新本地列表中的已读状态
                List<AlertItem> current = alerts.getValue();
                if (current != null) {
                    for (AlertItem item : current) {
                        if (item.getId() == alertId) {
                            // readStatus 是 final，需要替换整个列表
                            break;
                        }
                    }
                }
                loadAlerts(true);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }

    // ===== 自定义阈值 =====

    public void loadThresholds() {
        Long ghId = greenhouseId.getValue();
        if (ghId == null) return;

        repository.getThresholds(ghId, new GreenhouseRepository.Callback<List<ThresholdItem>>() {
            @Override
            public void onSuccess(List<ThresholdItem> data) {
                thresholds.postValue(data);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }

    public void saveThreshold(ThresholdItem threshold) {
        Long ghId = greenhouseId.getValue();
        if (ghId == null) return;

        threshold.setGreenhouseId(ghId);
        threshold.setEnabled(true);

        isLoading.setValue(true);
        repository.setThreshold(threshold, new GreenhouseRepository.Callback<ThresholdItem>() {
            @Override
            public void onSuccess(ThresholdItem data) {
                isLoading.postValue(false);
                thresholdMessage.postValue("阈值设置成功");
                loadThresholds();
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                thresholdMessage.postValue("设置失败: " + message);
            }
        });
    }
}
