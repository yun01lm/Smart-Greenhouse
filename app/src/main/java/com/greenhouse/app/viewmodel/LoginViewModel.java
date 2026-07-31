package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.local.TokenManager;
import com.greenhouse.app.data.model.LoginResponse;
import com.greenhouse.app.data.repository.AuthRepository;

/**
 * 登录 ViewModel
 * <p>
 * 处理登录业务逻辑。Activity 只负责 UI 展示和事件绑定。
 * 符合规范：Activity 不写业务逻辑。
 * </p>
 */
public class LoginViewModel extends ViewModel {

    private final AuthRepository repository;

    // 登录结果
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LoginViewModel() {
        this.repository = new AuthRepository();
    }

    public LiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    /**
     * 执行登录
     */
    public void login(String username, String password) {
        // 简单校验
        if (username == null || username.trim().isEmpty()) {
            errorMessage.setValue("请输入用户名");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("请输入密码");
            return;
        }

        isLoading.setValue(true);

        repository.login(username, password, new AuthRepository.Callback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse data) {
                // 保存 Token 和用户信息到 SharedPreferences
                TokenManager.saveToken(data.getToken());
                TokenManager.saveUserInfo(
                        data.getUserId(),
                        data.getUsername(),
                        data.getRole(),
                        data.getRealName()
                );

                isLoading.postValue(false);
                loginSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    /**
     * 检查是否已登录（Token 有效）
     */
    public boolean isLoggedIn() {
        return TokenManager.isLoggedIn();
    }
}
