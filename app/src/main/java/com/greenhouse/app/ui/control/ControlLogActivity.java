package com.greenhouse.app.ui.control;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.greenhouse.app.adapter.ControlLogAdapter;
import com.greenhouse.app.databinding.ActivityControlLogBinding;
import com.greenhouse.app.viewmodel.ControlLogViewModel;

/**
 * 设备控制记录页
 * <p>
 * 分页展示设备控制日志：设备、时间、操作人、动作、来源
 * （手动控制 / 场景触发 / 预警联动）、结果与触发场景，支持按来源筛选。
 * </p>
 */
public class ControlLogActivity extends AppCompatActivity {

    private ActivityControlLogBinding binding;
    private ControlLogViewModel viewModel;
    private ControlLogAdapter adapter;
    private boolean loadingMore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityControlLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long greenhouseId = getIntent().getLongExtra("greenhouse_id", 0);
        viewModel = new ViewModelProvider(this).get(ControlLogViewModel.class);
        viewModel.setCurrentGreenhouseId(greenhouseId);

        setupToolbar();
        setupRecyclerView();
        setupSourceFilter();
        observeData();

        viewModel.refresh();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ControlLogAdapter();
        binding.rvLogs.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLogs.setAdapter(adapter);
        binding.rvLogs.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || loadingMore) return;
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;
                int visibleCount = lm.getChildCount();
                int totalCount = lm.getItemCount();
                int firstVisible = lm.findFirstVisibleItemPosition();
                if (visibleCount + firstVisible >= totalCount - 3) {
                    loadingMore = true;
                    binding.llLoadingMore.setVisibility(View.VISIBLE);
                    viewModel.loadMore();
                }
            }
        });
    }

    private void setupSourceFilter() {
        binding.chipGroupSource.setOnCheckedStateChangeListener((group, checkedIds) -> {
            String source = null;
            if (checkedIds != null && !checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == binding.chipManual.getId()) source = "MANUAL";
                else if (id == binding.chipScene.getId()) source = "SCENE";
                else if (id == binding.chipAlert.getId()) source = "ALERT";
            }
            viewModel.setSource(source);
            viewModel.refresh();
        });
    }

    private void observeData() {
        viewModel.getLogs().observe(this, logs -> {
            adapter.setData(logs);
            loadingMore = false;
            binding.llLoadingMore.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setVisibility(
                    logs == null || logs.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (Boolean.TRUE.equals(loading)) {
                binding.progressBar.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });
    }
}
