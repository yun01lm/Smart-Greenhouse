package com.greenhouse.app.ui.expert;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.greenhouse.app.R;
import com.greenhouse.app.adapter.ChatMessageAdapter;
import com.greenhouse.app.databinding.ActivityChatBinding;
import com.greenhouse.app.viewmodel.ExpertViewModel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 聊天页 (F10)
 * <p>
 * 专家咨询的实时聊天界面。
 * 支持发送文字、图片、视频、环境快照。
 * 使用 WebSocket STOMP + REST 双通道通信。
 * </p>
 */
public class ChatActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_VIDEO_CAPTURE = 1002;

    private ActivityChatBinding binding;
    private ExpertViewModel viewModel;
    private ChatMessageAdapter adapter;
    private long conversationId;

    private File currentPhotoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        conversationId = getIntent().getLongExtra("conversation_id", 0);
        String expertName = getIntent().getStringExtra("expert_name");
        if (expertName != null) {
            binding.toolbar.setTitle(expertName);
        }

        viewModel = new ViewModelProvider(this).get(ExpertViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupInput();
        observeData();

        // 进入对话，加载历史消息并连接 WebSocket
        viewModel.enterConversation(conversationId);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // 环境快照按钮（Toolbar 右侧）
        binding.btnSnapshot.setOnClickListener(v -> {
            viewModel.sendSnapshot();
            Toast.makeText(this, "正在发送环境快照...", Toast.LENGTH_SHORT).show();
        });

        // 关闭对话按钮
        binding.btnClose.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("关闭对话")
                    .setMessage("关闭后将无法继续发送消息，确定关闭？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        viewModel.closeConversation();
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);
    }

    private void setupInput() {
        // 发送文字消息
        binding.btnSend.setOnClickListener(v -> {
            String content = binding.etInput.getText() != null
                    ? binding.etInput.getText().toString().trim() : "";
            if (!content.isEmpty()) {
                viewModel.sendTextMessage(content);
                binding.etInput.setText("");
            }
        });

        // 发送图片
        binding.btnImage.setOnClickListener(v -> dispatchTakePictureIntent());

        // 发送视频
        binding.btnVideo.setOnClickListener(v -> dispatchTakeVideoIntent());
    }

    private void observeData() {
        viewModel.getMessages().observe(this, messages -> {
            adapter.setData(messages);
            if (messages != null && !messages.isEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        viewModel.getWsConnected().observe(this, connected -> {
            if (connected != null && connected) {
                binding.toolbar.setSubtitle("● 在线");
            } else {
                binding.toolbar.setSubtitle("○ 离线（轮询中）");
            }
        });
    }

    // ===== 拍照（图片） =====

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                currentPhotoFile = createImageFile();
                Uri photoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", currentPhotoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException e) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    // ===== 录像 =====

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
            takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && currentPhotoFile != null && currentPhotoFile.exists()) {
                viewModel.sendImageMessage(currentPhotoFile);
                Toast.makeText(this, "正在发送图片...", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_VIDEO_CAPTURE && data != null && data.getData() != null) {
                // 将视频 URI 转为文件
                Uri videoUri = data.getData();
                if (videoUri != null) {
                    try {
                        File videoFile = new File(getCacheDir(), "video_" + System.currentTimeMillis() + ".mp4");
                        java.io.InputStream is = getContentResolver().openInputStream(videoUri);
                        java.io.FileOutputStream os = new java.io.FileOutputStream(videoFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        os.close();
                        is.close();
                        viewModel.sendVideoMessage(videoFile);
                        Toast.makeText(this, "正在发送视频...", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "处理视频文件失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
