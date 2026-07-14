package com.greenhouse.app.ui.expert;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;
import com.greenhouse.app.R;
import com.greenhouse.app.adapter.AuthorizationAdapter;
import com.greenhouse.app.data.model.AuthorizationInfo;
import com.greenhouse.app.databinding.ActivityAuthorizationBinding;
import com.greenhouse.app.viewmodel.ExpertViewModel;

/**
 * 授权管理页 (F10)
 * <p>
 * 使用 TabLayout + ViewPager2 展示两个标签页：
 * - 待处理：显示待审批的授权请求，支持同意/拒绝
 * - 已授权：显示已授权的列表，支持撤销
 * </p>
 */
public class AuthorizationActivity extends AppCompatActivity {

    private ActivityAuthorizationBinding binding;
    private ExpertViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthorizationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ExpertViewModel.class);

        setupToolbar();
        setupViewPager();
        observeData();

        // 加载数据
        viewModel.loadPendingAuthorizations();
        viewModel.loadActiveAuthorizations();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViewPager() {
        AuthorizationPagerAdapter pagerAdapter = new AuthorizationPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "待处理" : "已授权");
        }).attach();
    }

    private void observeData() {
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });
    }

    /**
     * ViewPager2 适配器
     */
    private class AuthorizationPagerAdapter extends FragmentStateAdapter {

        public AuthorizationPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return PendingAuthorizationFragment.newInstance();
            } else {
                return ActiveAuthorizationFragment.newInstance();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
