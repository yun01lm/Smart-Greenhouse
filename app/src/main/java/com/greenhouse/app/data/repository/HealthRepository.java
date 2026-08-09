package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.*;

import java.io.IOException;

import retrofit2.Response;

/**
 * 健康评估数据仓库
 * <p>负责健康评分、评分历史、评分详情查询。</p>
 */
public class HealthRepository extends BaseRepository {

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
}
