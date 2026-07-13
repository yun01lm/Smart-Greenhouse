package com.greenhouse.app.ui.common;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.greenhouse.app.R;
import com.greenhouse.app.databinding.ActivityMainBinding;
import com.greenhouse.app.ui.alert.AlertFragment;
import com.greenhouse.app.ui.dashboard.DashboardFragment;
import com.greenhouse.app.ui.diagnosis.DiagnosisFragment;
import com.greenhouse.app.ui.profile.ProfileFragment;

/**
 * 主界面
 * <p>
 * 底部 Tab 导航：看板 / 预警 / 诊断 / 问答 / 我的
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
            } else if (id == R.id.nav_alert) {
                fragment = new AlertFragment();
            } else if (id == R.id.nav_diagnosis) {
                fragment = new DiagnosisFragment();
            } else if (id == R.id.nav_qa) {
                // TODO: F04 AI问答
                fragment = new DashboardFragment(); // 占位
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }
            return true;
        });
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
