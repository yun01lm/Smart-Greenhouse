package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.HealthScoreData;
import com.greenhouse.app.data.model.PageResult;
import com.greenhouse.app.data.repository.HealthRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 多模态健康评分 ViewModel (F08)
 * <p>
 * 管理综合健康评分、子维度评分明细、历史趋势、详情报告等数据加载。
 * 符合规范：ViewModel 不持有 Context。
 * </p>
 */
public class HealthViewModel extends ViewModel {

    private final HealthRepository repository;

    // 当前综合评分
    private final MutableLiveData<HealthScoreData> currentScore = new MutableLiveData<>();
    // 详情报告（含 analysisJson）
    private final MutableLiveData<HealthScoreData> detailReport = new MutableLiveData<>();
    // 历史评分列表
    private final MutableLiveData<List<HealthScoreData>> historyList = new MutableLiveData<>(new ArrayList<>());
    // 选中展示的时间范围
    private final MutableLiveData<String> selectedRange = new MutableLiveData<>("7d");
    // 加载状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // 错误信息
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    // 是否还有更多历史数据
    private final MutableLiveData<Boolean> hasMoreHistory = new MutableLiveData<>(true);

    private long currentGreenhouseId;
    private int historyPage = 1;
    private static final int PAGE_SIZE = 10;

    public HealthViewModel() {
        this.repository = new HealthRepository();
    }

    // ===== LiveData =====

    public LiveData<HealthScoreData> getCurrentScore() { return currentScore; }
    public LiveData<HealthScoreData> getDetailReport() { return detailReport; }
    public LiveData<List<HealthScoreData>> getHistoryList() { return historyList; }
    public LiveData<String> getSelectedRange() { return selectedRange; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getHasMoreHistory() { return hasMoreHistory; }

    public void setCurrentGreenhouseId(long id) { this.currentGreenhouseId = id; }
    public long getCurrentGreenhouseId() { return currentGreenhouseId; }

    // ===== 加载综合评分 =====

    /**
     * 加载当前综合健康评分
     */
    public void loadCurrentScore() {
        isLoading.setValue(true);
        repository.getHealthScore(currentGreenhouseId,
                new HealthRepository.Callback<HealthScoreData>() {
                    @Override
                    public void onSuccess(HealthScoreData data) {
                        isLoading.postValue(false);
                        currentScore.postValue(data);
                        // 如果有评分ID，自动加载详情报告
                        if (data != null && data.getId() > 0) {
                            loadDetailReport(data.getId());
                        }
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("加载健康评分失败: " + message);
                    }
                });
    }

    // ===== 加载详情报告 =====

    /**
     * 加载指定ID的详细评估报告（含 analysisJson）
     */
    public void loadDetailReport(long scoreId) {
        repository.getHealthDetail(scoreId,
                new HealthRepository.Callback<HealthScoreData>() {
                    @Override
                    public void onSuccess(HealthScoreData data) {
                        detailReport.postValue(data);
                    }

                    @Override
                    public void onError(String message) {
                        // 详情加载失败不阻塞主流程，静默处理
                    }
                });
    }

    // ===== 历史趋势 =====

    /**
     * 切换时间范围并重新加载
     */
    public void selectTimeRange(String range) {
        selectedRange.setValue(range);
        refreshHistory();
    }

    /**
     * 刷新历史数据
     */
    public void refreshHistory() {
        historyPage = 1;
        hasMoreHistory.setValue(true);
        loadHistoryPage();
    }

    /**
     * 加载更多历史数据
     */
    public void loadMoreHistory() {
        if (Boolean.TRUE.equals(hasMoreHistory.getValue())) {
            historyPage++;
            loadHistoryPage();
        }
    }

    private void loadHistoryPage() {
        isLoading.setValue(true);
        repository.getHealthHistory(currentGreenhouseId, historyPage, PAGE_SIZE,
                new HealthRepository.Callback<PageResult<HealthScoreData>>() {
                    @Override
                    public void onSuccess(PageResult<HealthScoreData> data) {
                        isLoading.postValue(false);
                        List<HealthScoreData> list = data.getList();
                        if (list == null) list = new ArrayList<>();

                        if (historyPage == 1) {
                            historyList.postValue(list);
                        } else {
                            List<HealthScoreData> current = historyList.getValue();
                            if (current == null) current = new ArrayList<>();
                            current.addAll(list);
                            historyList.postValue(current);
                        }

                        boolean more = list.size() >= PAGE_SIZE;
                        hasMoreHistory.postValue(more);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("加载历史趋势失败: " + message);
                    }
                });
    }

    // ===== 全部加载 =====

    /**
     * 一次加载全部数据（入口触发）
     */
    public void loadAll() {
        loadCurrentScore();
        refreshHistory();
    }

    /**
     * 清除错误
     */
    public void clearError() {
        errorMessage.postValue(null);
    }
}
