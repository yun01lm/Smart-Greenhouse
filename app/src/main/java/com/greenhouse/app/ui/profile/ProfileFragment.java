package com.greenhouse.app.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.greenhouse.app.data.local.TokenManager;
import com.greenhouse.app.databinding.FragmentProfileBinding;
import com.greenhouse.app.ui.expert.AuthorizationActivity;
import com.greenhouse.app.ui.expert.ExpertListActivity;
import com.greenhouse.app.ui.employee.EmployeeManagementActivity;
import com.greenhouse.app.ui.login.LoginActivity;
import com.greenhouse.app.util.RoleAdapter;

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
            case "TECHNICIAN": roleText = "技术员"; break;
            case "EXPERT": roleText = "专家"; break;
            case "ADMIN": roleText = "管理员"; break;
            default: roleText = role;
        }
        binding.tvRole.setText(roleText);

        // 员工管理入口 (R26 棚主专属)
        binding.btnEmployeeManagement.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), EmployeeManagementActivity.class);
            startActivity(intent);
        });

        // 专家咨询入口 (F10)
        binding.btnExpertConsult.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), ExpertListActivity.class);
            startActivity(intent);
        });

        // 授权管理入口 (F10)
        binding.btnAuthorization.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AuthorizationActivity.class);
            startActivity(intent);
        });

        // 退出登录
        binding.btnLogout.setOnClickListener(v -> {
            TokenManager.clear();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        // ===== F11 角色适配 =====
        applyRoleAdapter();
    }

    /**
     * F11 角色适配：根据角色显示/隐藏功能入口。
     * <p>
     * 棚主：显示员工管理入口（如有），隐藏授权管理入口（棚主不需要授权）。
     * 员工：隐藏员工管理入口，显示授权管理入口。专家咨询入口由 canAskExpert 权限控制。
     * </p>
     */
    private void applyRoleAdapter() {
        if (RoleAdapter.isOwner()) {
            // 棚主：显示员工管理入口（R26），隐藏授权管理（棚主是授权的审批方，在 Web 端管理）
            binding.btnEmployeeManagement.setVisibility(View.VISIBLE);
            binding.btnAuthorization.setVisibility(View.GONE);
            // 专家咨询：棚主默认可用
            binding.btnExpertConsult.setVisibility(View.VISIBLE);
        } else {
            // 非棚主（普通员工/技术员/专家/管理员）：隐藏员工管理入口
            binding.btnEmployeeManagement.setVisibility(View.GONE);
        }
        if (RoleAdapter.isWorker()) {
            // 员工：专家咨询入口由权限控制
            if (!RoleAdapter.canAskExpert()) {
                binding.btnExpertConsult.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
