package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.*;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * 长势评估数据仓库
 * <p>负责长势评估、长势历史、截帧图片、作物周期。</p>
 */
public class GrowthRepository extends BaseRepository {

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
}
