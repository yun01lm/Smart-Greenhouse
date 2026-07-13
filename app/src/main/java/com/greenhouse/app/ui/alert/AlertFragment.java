package com.greenhouse.app.ui.alert;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.greenhouse.app.adapter.AlertAdapter;
import com.greenhouse.app.data.model.AlertItem;
import com.greenhouse.app.databinding.FragmentAlertBinding;
import com.greenhouse.app.viewmodel.AlertViewModel;

/**
 * 环境预警中心 Fragment (F02)
 * <p>
 * 展示预警列表，支持按级别筛选，点击进入详情。
 * 符合规范：Fragment 不写业务逻辑。
 * </p>
 */
public class AlertFragment extends Fragment {

    private FragmentAlertBinding binding;
    private AlertViewModel viewModel;
    private AlertAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AlertViewModel.class);

        // RecyclerView
        adapter = new AlertAdapter();
        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAlerts.setAdapter(adapter);

        // 点击预警 → 详情页
        adapter.setOnItemClickListener(alert -> {
            Intent intent = new Intent(requireActivity(), AlertDetailActivity.class);
            intent.putExtra("alert_id", alert.getId());
            intent.putExtra("alert_title", alert.getTitle());
            intent.putExtra("alert_content", alert.getContent());
            intent.putExtra("alert_level", alert.getLevel());
            intent.putExtra("alert_time", alert.getCreatedAt());
            intent.putExtra("sensor_type", alert.getSensorType());
            intent.putExtra("sensor_value", alert.getSensorValue() != null ? alert.getSensorValue() : 0);
            startActivity(intent);
        });

        // 筛选芯片
        binding.chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) viewModel.clearFilter();
        });
        binding.chipWarning.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) viewModel.filterByLevel("WARNING");
        });
        binding.chipCritical.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) viewModel.filterByLevel("CRITICAL");
        });

        // 阈值设置按钮
        binding.btnThreshold.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), ThresholdSettingsActivity.class));
        });

        // 观察预警列表
        viewModel.getAlerts().observe(getViewLifecycleOwner(), alerts -> {
            adapter.setData(alerts);
            if (alerts == null || alerts.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.rvAlerts.setVisibility(View.GONE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.rvAlerts.setVisibility(View.VISIBLE);
            }
        });

        // 观察加载状态
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
        });

        // 初始化：从 DashboardViewModel 共享的大棚ID（通过 Activity 传递或 SharedPreferences）
        // 简化：默认大棚ID=1，实际使用时从 MainActivity 获取当前选中大棚
        viewModel.init(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
