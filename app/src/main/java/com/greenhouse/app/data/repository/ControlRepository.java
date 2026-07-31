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

    public void getActuators(long greenhouseId, Callback<List<ActuatorInfo>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<ActuatorInfo>>> response =
                        apiService.getActuators(greenhouseId).execute();
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

    public void controlActuator(long actuatorId, String action, long greenhouseId,
                                Callback<ControlResponse> callback) {
        execute(() -> {
            try {
                ControlRequest request = new ControlRequest(actuatorId, action, greenhouseId);
                Response<ApiResponse<ControlResponse>> response = apiService.controlActuator(request).execute();
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

    public void executeScene(long sceneId, long greenhouseId, Callback<ControlResponse> callback) {
        execute(() -> {
            try {
                SceneExecuteRequest request = new SceneExecuteRequest(sceneId, greenhouseId);
                Response<ApiResponse<ControlResponse>> response = apiService.executeScene(request).execute();
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
