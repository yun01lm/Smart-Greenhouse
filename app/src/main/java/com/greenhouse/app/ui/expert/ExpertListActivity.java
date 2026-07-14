package com.greenhouse.app.ui.expert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.greenhouse.app.R;
import com.greenhouse.app.adapter.ExpertAdapter;
import com.greenhouse.app.data.model.ExpertInfo;
import com.greenhouse.app.databinding.ActivityExpertListBinding;
import com.greenhouse.app.viewmodel.ExpertViewModel;

/**
 * 专家列表页 (F10)
 * <p>
 * 展示所有专家的列表，支持按在线状态筛选。
 * 点击"求助"弹出对话框输入问题主题，发起咨询。
 * </p>
 */
public class ExpertListActivity extends AppCompatActivity {

    private ActivityExpertListBinding binding;
    private ExpertViewModel viewModel;
    private ExpertAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExpertListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 接收传递的 greenhouseId
        long greenhouseId = getIntent().getLongExtra("greenhouse_id", 0);
        viewModel = new ViewModelProvider(this).get(ExpertViewModel.class);
        viewModel.setCurrentGreenhouseId(greenhouseId);

        setupToolbar();
        setupRecyclerView();
        observeData();
        viewModel.loadExperts();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ExpertAdapter();
        binding.rvExperts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvExperts.setAdapter(adapter);

        adapter.setOnExpertActionListener(expert -> showConsultDialog(expert));
    }

    private void observeData() {
        viewModel.getExperts().observe(this, experts -> {
            adapter.setData(experts);
            binding.progressBar.setVisibility(View.GONE);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
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

    /**
     * 显示发起咨询对话框
     */
    private void showConsultDialog(ExpertInfo expert) {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("请输入问题主题（如：番茄叶片发黄）");
        input.setPadding(16, 16, 16, 16);

        new MaterialAlertDialogBuilder(this)
                .setTitle("向 " + expert.getRealName() + " 求助")
                .setMessage(expert.getExpertSpecialty())
                .setView(input)
                .setPositiveButton("发起求助", (dialog, which) -> {
                    String subject = input.getText() != null ? input.getText().toString().trim() : "";
                    if (subject.isEmpty()) {
                        Toast.makeText(this, "请输入问题主题", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.createConversation(expert.getId(), subject);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
