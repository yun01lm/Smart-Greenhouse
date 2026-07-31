package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.CropCycleData;
import com.greenhouse.app.data.model.GrowthAssessment;
import com.greenhouse.app.data.model.GrowthImage;
import com.greenhouse.app.data.model.PageResult;
import com.greenhouse.app.data.repository.GrowthRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 作物长势评估 ViewModel
 * <p>
 * 管理最新长势评估、历史记录、截帧图片列表和种植周期信息的加载。
 * 符合规范：ViewModel 不持有 Context，网络请求在 Repository 子线程执行。
 * </p>
 */
public class GrowthViewModel extends ViewModel {

    private final GrowthRepository repository;

    // 最新长势评估
    private final MutableLiveData<GrowthAssessment> latestAssessment = new MutableLiveData<>();
    // 长势历史记录列表
    private final MutableLiveData<List<GrowthAssessment>> historyList = new MutableLiveData<>(new ArrayList<>());
    // 截帧图片列表
    private final MutableLiveData<List<GrowthImage>> imageList = new MutableLiveData<>(new ArrayList<>());
    // 当前种植周期信息
    private final MutableLiveData<List<CropCycleData>> cropCycles = new MutableLiveData<>(new ArrayList<>());
    // 选中的种植周期
    private final MutableLiveData<CropCycleData> activeCropCycle = new MutableLiveData<>();
    // 加载状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // 错误信息
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    // 是否还有更多历史数据
    private final MutableLiveData<Boolean> hasMoreHistory = new MutableLiveData<>(true);
    // 是否还有更多图片
    private final MutableLiveData<Boolean> hasMoreImages = new MutableLiveData<>(true);

    private long currentGreenhouseId;
    private int historyPage = 1;
    private int imagePage = 1;
    private static final int PAGE_SIZE = 10;

    public GrowthViewModel() {
        this.repository = new GrowthRepository();
    }

    // ===== LiveData =====

    public LiveData<GrowthAssessment> getLatestAssessment() { return latestAssessment; }
    public LiveData<List<GrowthAssessment>> getHistoryList() { return historyList; }
    public LiveData<List<GrowthImage>> getImageList() { return imageList; }
    public LiveData<List<CropCycleData>> getCropCycles() { return cropCycles; }
    public LiveData<CropCycleData> getActiveCropCycle() { return activeCropCycle; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getHasMoreHistory() { return hasMoreHistory; }
    public LiveData<Boolean> getHasMoreImages() { return hasMoreImages; }

    public void setCurrentGreenhouseId(long id) {
        this.currentGreenhouseId = id;
    }

    public long getCurrentGreenhouseId() {
        return currentGreenhouseId;
    }

    // ===== 加载最新长势评估 =====

    /**
     * 加载最新一次长势评估结果
     */
    public void loadLatestAssessment() {
        isLoading.setValue(true);
        repository.getLatestGrowthAssessment(currentGreenhouseId,
                new GrowthRepository.Callback<GrowthAssessment>() {
                    @Override
                    public void onSuccess(GrowthAssessment data) {
                        isLoading.postValue(false);
                        latestAssessment.postValue(data);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("加载长势评估失败: " + message);
                    }
                });
    }

    // ===== 加载长势历史（分页） =====

    /**
     * 加载长势评估历史记录（首页/刷新）
     */
    public void refreshHistory() {
        historyPage = 1;
        hasMoreHistory.setValue(true);
        loadHistoryPage();
    }

    /**
     * 加载更多历史记录（分页）
     */
    public void loadMoreHistory() {
        if (Boolean.TRUE.equals(hasMoreHistory.getValue())) {
            historyPage++;
            loadHistoryPage();
        }
    }

    private void loadHistoryPage() {
        isLoading.setValue(true);
        repository.getGrowthHistory(currentGreenhouseId, historyPage, PAGE_SIZE,
                new GrowthRepository.Callback<PageResult<GrowthAssessment>>() {
                    @Override
                    public void onSuccess(PageResult<GrowthAssessment> data) {
                        isLoading.postValue(false);
                        List<GrowthAssessment> list = data.getList();
                        if (list == null) list = new ArrayList<>();

                        if (historyPage == 1) {
                            historyList.postValue(list);
                        } else {
                            // 追加到现有列表
                            List<GrowthAssessment> current = historyList.getValue();
                            if (current == null) current = new ArrayList<>();
                            current.addAll(list);
                            historyList.postValue(current);
                        }

                        // 判断是否还有更多
                        boolean more = list.size() >= PAGE_SIZE;
                        hasMoreHistory.postValue(more);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("加载历史记录失败: " + message);
                    }
                });
    }

    // ===== 加载截帧图片（分页） =====

    /**
     * 加载截帧图片列表（首页/刷新）
     */
    public void refreshImages() {
        imagePage = 1;
        hasMoreImages.setValue(true);
        loadImagesPage();
    }

    /**
     * 加载更多截帧图片（分页）
     */
    public void loadMoreImages() {
        if (Boolean.TRUE.equals(hasMoreImages.getValue())) {
            imagePage++;
            loadImagesPage();
        }
    }

    private void loadImagesPage() {
        isLoading.setValue(true);
        // 默认加载当天的截帧
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        repository.getGrowthImages(currentGreenhouseId, today, imagePage, PAGE_SIZE,
                new GrowthRepository.Callback<PageResult<GrowthImage>>() {
                    @Override
                    public void onSuccess(PageResult<GrowthImage> data) {
                        isLoading.postValue(false);
                        List<GrowthImage> list = data.getList();
                        if (list == null) list = new ArrayList<>();

                        if (imagePage == 1) {
                            imageList.postValue(list);
                        } else {
                            List<GrowthImage> current = imageList.getValue();
                            if (current == null) current = new ArrayList<>();
                            current.addAll(list);
                            imageList.postValue(current);
                        }

                        boolean more = list.size() >= PAGE_SIZE;
                        hasMoreImages.postValue(more);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("加载截帧图片失败: " + message);
                    }
                });
    }

    // ===== 加载种植周期信息 =====

    /**
     * 加载当前大棚的种植周期列表
     */
    public void loadCropCycles() {
        repository.getCropCycles(currentGreenhouseId,
                new GrowthRepository.Callback<List<CropCycleData>>() {
                    @Override
                    public void onSuccess(List<CropCycleData> data) {
                        if (data != null && !data.isEmpty()) {
                            cropCycles.postValue(data);
                            // 默认选中第一个活跃周期
                            for (CropCycleData cycle : data) {
                                if ("ACTIVE".equals(cycle.getStatus())) {
                                    activeCropCycle.postValue(cycle);
                                    return;
                                }
                            }
                            // 没有活跃的，选第一个
                            activeCropCycle.postValue(data.get(0));
                        } else {
                            cropCycles.postValue(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onError(String message) {
                        errorMessage.postValue("加载种植周期失败: " + message);
                    }
                });
    }

    /**
     * 加载全部数据（看板入口触发）
     */
    public void loadAll() {
        loadLatestAssessment();
        refreshImages();
        loadCropCycles();
    }

    /**
     * 清除错误
     */
    public void clearError() {
        errorMessage.postValue(null);
    }
}
