package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.*;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * 设备控制数据仓库
 * <p>负责执行器控制、场景管理。</p>
 */
public class ControlRepository extends BaseRepository {

    public void getDevices(long greenhouseId, Callback<List<DeviceInfo>> callback) {
        execute(() -> {
            try {
                // 控制页只展示控制器类设备
                Response<ApiResponse<List<DeviceInfo>>> response =
                        apiService.getDevices(greenhouseId, "CONTROLLER").execute();
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

    public void controlActuator(long deviceId, String action,
                                Callback<DeviceControlResult> callback) {
        execute(() -> {
            try {
                ControlRequest request = new ControlRequest(deviceId, action);
                Response<ApiResponse<DeviceControlResult>> response = apiService.controlActuator(request).execute();
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

    public void getScenes(long greenhouseId, Callback<List<SceneInfo>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<SceneInfo>>> response =
                        apiService.getScenes(greenhouseId).execute();
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

    public void executeScene(long sceneId, Callback<List<DeviceControlResult>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<DeviceControlResult>>> response =
                        apiService.executeScene(sceneId).execute();
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
