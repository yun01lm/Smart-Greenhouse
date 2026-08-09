package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.DiagnosisHistoryItem;
import com.greenhouse.app.data.model.DiagnosisResponse;
import com.greenhouse.app.data.model.PageResult;
import com.greenhouse.app.data.repository.DiagnosisRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 病虫害诊断业务逻辑
 * <p>
 * 符合规范：Activity/Fragment 不写业务逻辑，全部在此 ViewModel 中。
 * ViewModel 不持有 Context/ContentResolver 等 Android 组件引用。
 * </p>
 */
public class DiagnosisViewModel extends ViewModel {

    private final DiagnosisRepository repository;

    // 诊断结果
    private final MutableLiveData<DiagnosisResponse> diagnosisResult = new MutableLiveData<>();
    // 诊断历史列表
    private final MutableLiveData<List<DiagnosisHistoryItem>> historyList = new MutableLiveData<>();
    // 加载状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // 错误信息
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long currentGreenhouseId;
    private int currentPage = 1;
    private boolean hasMore = true;
    private static final int PAGE_SIZE = 20;

    public DiagnosisViewModel() {
        this.repository = new DiagnosisRepository();
    }

    // ===== LiveData 暴露 =====

    public LiveData<DiagnosisResponse> getDiagnosisResult() { return diagnosisResult; }
    public LiveData<List<DiagnosisHistoryItem>> getHistoryList() { return historyList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void setCurrentGreenhouseId(long id) {
        this.currentGreenhouseId = id;
        currentPage = 1;
        hasMore = true;
    }

    public long getCurrentGreenhouseId() {
        return currentGreenhouseId;
    }

    public boolean hasMore() {
        return hasMore;
    }

    // ===== 诊断上传 =====

    /**
     * 上传图片进行病虫害诊断
     * <p>
     * Fragment 负责将 Uri 压缩为 File 后调用此方法。
     * </p>
     * @param imageFile    已压缩的图片文件
     * @param greenhouseId 大棚ID
     */
    public void diagnose(File imageFile, long greenhouseId) {
        isLoading.postValue(true);
        repository.diagnose(imageFile, greenhouseId, new DiagnosisRepository.Callback<DiagnosisResponse>() {
            @Override
            public void onSuccess(DiagnosisResponse data) {
                isLoading.postValue(false);
                diagnosisResult.postValue(data);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // ===== 诊断历史 =====

    /**
     * 加载诊断历史（首页/刷新）
     */
    public void loadHistory(long greenhouseId) {
        currentPage = 1;
        hasMore = true;
        loadHistoryPage(greenhouseId, currentPage);
    }

    /**
     * 加载更多历史（分页）
     */
    public void loadMoreHistory() {
        if (!hasMore) return;
        currentPage++;
        loadHistoryPage(currentGreenhouseId, currentPage);
    }

    private void loadHistoryPage(long greenhouseId, int page) {
        isLoading.postValue(true);
        repository.getDiagnosisHistory(greenhouseId, page, PAGE_SIZE,
                new DiagnosisRepository.Callback<PageResult<DiagnosisHistoryItem>>() {
            @Override
            public void onSuccess(PageResult<DiagnosisHistoryItem> data) {
                isLoading.postValue(false);
                List<DiagnosisHistoryItem> currentList = historyList.getValue();
                if (currentList == null || page == 1) {
                    currentList = new ArrayList<>();
                }
                if (data.getList() != null) {
                    currentList.addAll(data.getList());
                    hasMore = data.getList().size() >= PAGE_SIZE;
                } else {
                    hasMore = false;
                }
                historyList.postValue(currentList);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                // 加载更多失败时回退页码
                if (page > 1) currentPage--;
                errorMessage.postValue(message);
            }
        });
    }

    /**
     * 判断是否需要引导求助专家（置信度 < 70%）
     */
    public boolean needExpertHelp(DiagnosisResponse result) {
        return result.getNeedExpert() != null && result.getNeedExpert();
    }
}
