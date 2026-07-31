package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.ApiResponse;
import com.greenhouse.app.data.model.LoginRequest;
import com.greenhouse.app.data.model.LoginResponse;
import com.greenhouse.app.data.model.UserInfo;

import java.io.IOException;

import retrofit2.Response;

/**
 * 认证相关数据仓库
 * <p>负责登录、注册、获取当前用户信息。</p>
 */
public class AuthRepository extends BaseRepository {

    public void login(String username, String password, Callback<LoginResponse> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<LoginResponse>> response =
                        apiService.login(new LoginRequest(username, password)).execute();
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

    public void getCurrentUser(Callback<UserInfo> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<UserInfo>> response =
                        apiService.getCurrentUser().execute();
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
