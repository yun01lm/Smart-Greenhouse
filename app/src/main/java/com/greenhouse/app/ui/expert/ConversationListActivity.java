package com.greenhouse.app.ui.expert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.greenhouse.app.adapter.ConversationAdapter;
import com.greenhouse.app.data.model.ConversationInfo;
import com.greenhouse.app.databinding.ActivityConversationListBinding;
import com.greenhouse.app.viewmodel.ExpertViewModel;

/**
 * 我的咨询（会话列表页）
 * <p>展示当前用户发起的所有专家咨询，点击进入聊天页继续对话。</p>
 */
public class ConversationListActivity extends AppCompatActivity {

    private ActivityConversationListBinding binding;
    private ExpertViewModel viewModel;
    private ConversationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConversationListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long greenhouseId = getIntent().getLongExtra("greenhouse_id", 0);
        viewModel = new ViewModelProvider(this).get(ExpertViewModel.class);
        viewModel.setCurrentGreenhouseId(greenhouseId);

        setupToolbar();
        setupRecyclerView();
        observeData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到列表时刷新（新消息/未读数变化）
        viewModel.refreshConversations();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter();
        binding.rvConversations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConversations.setAdapter(adapter);

        adapter.setOnConversationClickListener(conversation -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("conversation_id", conversation.getId());
            intent.putExtra("expert_name", conversation.getExpertName());
            intent.putExtra("greenhouse_id", conversation.getGreenhouseId());
            startActivity(intent);
        });
    }

    private void observeData() {
        viewModel.getConversations().observe(this, conversations -> {
            adapter.setData(conversations);
            binding.progressBar.setVisibility(View.GONE);
            binding.tvEmpty.setVisibility(
                    conversations == null || conversations.isEmpty() ? View.VISIBLE : View.GONE);
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
}