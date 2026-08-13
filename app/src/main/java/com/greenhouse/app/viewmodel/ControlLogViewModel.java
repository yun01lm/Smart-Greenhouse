package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.ControlLogItem;
import com.greenhouse.app.data.repository.ControlRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备控制记录 ViewModel
 * <p>
 * 分页加载控制日志，支持按来源筛选（MANUAL / SCENE / ALERT）。
 * 符合规范：ViewModel 不持有 Context，网络请求在 Repository 子线程执行。
 * </p>
 */
public class ControlLogViewModel extends ViewModel {

    private final ControlRepository repository;

    private final MutableLiveData<List<ControlLogItem>> logs = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long currentGreenhouseId;
    private String currentSource; // null = 全部来源
    private int page = 0;
    private static final int PAGE_SIZE = 20;

    public ControlLogViewModel() {
        this.repository = new ControlRepository();
    }

    public LiveData<List<ControlLogItem>> getLogs() { return logs; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getHasMore() { return hasMore; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void setCurrentGreenhouseId(long id) { this.currentGreenhouseId = id; }

    public void setSource(String source) {
        this.currentSource = source;
    }

    public void clearError() { errorMessage.setValue(null); }

    /** 刷新（回到第一页） */
    public void refresh() {
        page = 0;
        loadPage();
    }

    /** 加载下一页 */
    public void loadMore() {
        if (Boolean.TRUE.equals(hasMore.getValue()) && !Boolean.TRUE.equals(isLoading.getValue())) {
            loadPage();
        }
    }

    private void loadPage() {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        isLoading.setValue(true);
        final int loadPage = page;
        repository.getControlLogs(currentGreenhouseId, currentSource, loadPage, PAGE_SIZE,
                new ControlRepository.Callback<List<ControlLogItem>>() {
                    @Override
                    public void onSuccess(List<ControlLogItem> data) {
                        List<ControlLogItem> list = data != null ? data : new ArrayList<>();
                        if (loadPage == 0) {
                            logs.postValue(list);
                        } else {
                            List<ControlLogItem> merged = new ArrayList<>();
                            List<ControlLogItem> existing = logs.getValue();
                            if (existing != null) merged.addAll(existing);
                            merged.addAll(list);
                            logs.postValue(merged);
                        }
                        page = loadPage + 1;
                        hasMore.postValue(list.size() >= PAGE_SIZE);
                        isLoading.postValue(false);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("加载控制记录失败: " + message);
                    }
                });
    }
}
