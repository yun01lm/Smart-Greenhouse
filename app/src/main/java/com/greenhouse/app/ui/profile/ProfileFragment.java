package com.greenhouse.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.greenhouse.app.data.local.TokenManager;
import com.greenhouse.app.databinding.FragmentProfileBinding;
import com.greenhouse.app.ui.login.LoginActivity;

/**
 * 个人中心 Fragment (F09)
 * <p>
 * 展示用户信息、角色，提供退出登录功能。
 * 符合规范：Fragment 不写业务逻辑。
 * </p>
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 显示用户信息（从 SharedPreferences 读取）
        String realName = TokenManager.getRealName();
        String role = TokenManager.getRole();
        String username = TokenManager.getUsername();

        binding.tvRealName.setText(realName != null && !realName.isEmpty() ? realName : username);
        binding.tvUsername.setText("@" + username);

        // 角色中文显示
        String roleText;
        switch (role) {
            case "OWNER": roleText = "棚主"; break;
            case "WORKER": roleText = "员工"; break;
            case "EXPERT": roleText = "专家"; break;
            case "ADMIN": roleText = "管理员"; break;
            default: roleText = role;
        }
        binding.tvRole.setText(roleText);

        // 退出登录
        binding.btnLogout.setOnClickListener(v -> {
            TokenManager.clear();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
