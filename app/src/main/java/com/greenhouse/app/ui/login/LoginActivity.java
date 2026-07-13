package com.greenhouse.app.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.greenhouse.app.databinding.ActivityLoginBinding;
import com.greenhouse.app.ui.common.MainActivity;
import com.greenhouse.app.viewmodel.LoginViewModel;

/**
 * 登录页面
 * <p>
 * 职责：UI 事件绑定 + 导航。不写业务逻辑（全部在 ViewModel 中）。
 * 符合规范：Activity 不写业务逻辑。
 * </p>
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // 检查是否已登录
        if (viewModel.isLoggedIn()) {
            navigateToMain();
            return;
        }

        // 观察登录结果
        viewModel.getLoginSuccess().observe(this, success -> {
            if (success != null && success) {
                navigateToMain();
            }
        });

        // 观察错误消息
        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                binding.tvError.setText(msg);
                binding.tvError.setVisibility(View.VISIBLE);
            }
        });

        // 观察加载状态
        viewModel.getIsLoading().observe(this, loading -> {
            binding.progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(loading == null || !loading);
        });

        // 登录按钮点击
        binding.btnLogin.setOnClickListener(v -> {
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            binding.tvError.setVisibility(View.GONE);
            viewModel.login(username, password);
        });
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
