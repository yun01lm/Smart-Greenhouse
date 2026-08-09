package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

/**
 * 预警数据仓库
 * <p>负责预警列表、已读标记、未读计数、阈值管理。</p>
 */
public class AlertRepository extends BaseRepository {

    public void getAlerts(long greenhouseId, int page, int size, Callback<PageResult<AlertItem>> callback) {
        getAlerts(greenhouseId, page, size, null, callback);
    }

    public void getAlerts(long greenhouseId, int page, int size, String level,
                          Callback<PageResult<AlertItem>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<PageResult<AlertItem>>> response;
                if (level != null && !level.isEmpty()) {
                    response = apiService.getAlertsByLevel(greenhouseId, page, size, level).execute();
                } else {
                    response = apiService.getAlerts(greenhouseId, page, size).execute();
                }
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

    public void markAlertRead(long alertId, Callback<Void> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<Void>> response = apiService.markAlertRead(alertId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, null);
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void getUnreadAlertCount(long greenhouseId, Callback<Integer> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<Map<String, Object>>> response =
                        apiService.getUnreadAlertCount(greenhouseId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> data = response.body().getData();
                    int count = 0;
                    if (data != null && data.get("count") instanceof Number) {
                        count = ((Number) data.get("count")).intValue();
                    }
                    postSuccess(callback, count);
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    public void getThresholds(long greenhouseId, Callback<List<ThresholdItem>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<ThresholdItem>>> response =
                        apiService.getThresholds(greenhouseId).execute();
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

    public void setThreshold(ThresholdItem threshold, Callback<ThresholdItem> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<ThresholdItem>> response =
                        apiService.setThreshold(threshold).execute();
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

    public void deleteThreshold(long id, Callback<Void> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<Void>> response = apiService.deleteThreshold(id).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, null);
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }
}
