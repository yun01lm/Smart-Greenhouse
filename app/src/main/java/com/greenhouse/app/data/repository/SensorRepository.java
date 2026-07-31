package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.*;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * 传感器数据仓库
 * <p>负责实时数据、历史数据查询。</p>
 */
public class SensorRepository extends BaseRepository {

    public void getRealtimeData(long greenhouseId, Callback<SensorRealtimeData> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<SensorRealtimeData>> response =
                        apiService.getRealtimeData(greenhouseId).execute();
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

    public void getGreenhouses(Callback<List<Greenhouse>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<Greenhouse>>> response =
                        apiService.getGreenhouses().execute();
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

    public void getHistoryData(long greenhouseId, String sensorType,
                               long startTime, long endTime,
                               Callback<HistoryResponse> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<HistoryResponse>> response =
                        apiService.getHistoryData(greenhouseId, sensorType, startTime, endTime).execute();
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
