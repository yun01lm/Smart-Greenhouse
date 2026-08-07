package com.greenhouse.app.ui.common;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.greenhouse.app.R;
import com.greenhouse.app.databinding.ActivityMainBinding;
import com.greenhouse.app.ui.assistant.AiAssistantFragment;
import com.greenhouse.app.ui.control.ControlFragment;
import com.greenhouse.app.ui.dashboard.DashboardFragment;
import com.greenhouse.app.ui.profile.ProfileFragment;
import com.greenhouse.app.util.RoleAdapter;

/**
 * 主界面
 * <p>
 * 底部 Tab 导航：看板(含预警) / AI助手(诊断+问答) / 设备控制 / 我的
 * 符合规范：Activity 只负责导航，不写业务逻辑。
 * </p>
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 默认显示看板
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        // 底部导航切换
        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (id == R.id.nav_assistant) {
                fragment = new AiAssistantFragment();
            } else if (id == R.id.nav_control) {
                fragment = new ControlFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }
            return true;
        });

        // ===== F11 角色适配：员工按权限隐藏底部 Tab =====
        applyRoleTabFilter();
    }

    /**
     * F11 角色适配：根据当前用户权限，隐藏底部导航中无权限的 Tab。
     * <p>
     * 棚主：全部 Tab 可见。
     * 员工：根据权限位控制 AI 助手 Tab 和设备控制 Tab 的可见性。
     * 看板 Tab 和"我的" Tab 始终可见（看板中已通过卡片级权限控制具体功能）。
     * </p>
     */
    private void applyRoleTabFilter() {
        // 棚主/技术员：全部 Tab 可见（R23）
        if (RoleAdapter.isOwner() || RoleAdapter.isTechnician()) return;

        // 员工：AI 助手 Tab 可见性
        if (!RoleAdapter.isAiAssistantVisible()) {
            binding.bottomNav.getMenu().findItem(R.id.nav_assistant).setVisible(false);
        }

        // 员工：设备控制 Tab 可见性
        if (!RoleAdapter.isControlVisible()) {
            binding.bottomNav.getMenu().findItem(R.id.nav_control).setVisible(false);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
