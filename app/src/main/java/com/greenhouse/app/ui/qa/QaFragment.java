package com.greenhouse.app.ui.qa;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.greenhouse.app.R;
import com.greenhouse.app.adapter.ChatAdapter;
import com.greenhouse.app.databinding.FragmentQaBinding;
import com.greenhouse.app.viewmodel.QaViewModel;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * AI 智能问答主页
 * <p>
 * 支持文字和语音两种输入方式，对话以聊天气泡展示。
 * 符合规范：Fragment 只负责 UI，业务逻辑在 ViewModel。
 * </p>
 */
public class QaFragment extends Fragment {

    private FragmentQaBinding binding;
    private QaViewModel viewModel;
    private ChatAdapter adapter;
    private TextToSpeech tts;

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;

    private ActivityResultLauncher<String> audioPermissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentQaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(QaViewModel.class);

        // TTS 初始化
        tts = new TextToSpeech(requireContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.CHINESE);
            }
        });
        viewModel.setTts(tts);

        // RecyclerView
        adapter = new ChatAdapter();
        binding.rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChat.setHasFixedSize(true);
        binding.rvChat.setAdapter(adapter);

        adapter.setOnTtsClickListener(text -> viewModel.speakAnswer(text));

        // 权限
        audioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) startRecording();
                    else Toast.makeText(requireContext(), "需要录音权限才能使用语音输入", Toast.LENGTH_SHORT).show();
                });

        // 发送按钮
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etQuestion.getText().toString().trim();
            if (!text.isEmpty()) {
                binding.etQuestion.setText("");
                viewModel.askQuestion(text);
            }
        });

        // 语音按钮
        binding.btnVoice.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                requestAudioPermission();
            }
        });

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                binding.rvChat.smoothScrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsSpeaking().observe(getViewLifecycleOwner(), speaking -> {
            // TTS 状态变化时更新语音按钮
        });
    }

    // ===== 录音 =====

    private void requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startRecording() {
        try {
            audioFile = File.createTempFile("qa_voice_", ".aac", requireContext().getCacheDir());
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            binding.btnVoice.setImageResource(android.R.drawable.ic_media_pause);
            Toast.makeText(requireContext(), "正在录音...再次点击停止", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "录音启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            binding.btnVoice.setImageResource(R.drawable.ic_mic);

            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                viewModel.askVoice(audioFile);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "录音保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    public void setGreenhouseId(long greenhouseId) {
        if (viewModel != null) {
            viewModel.setCurrentGreenhouseId(greenhouseId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaRecorder != null) {
            try { mediaRecorder.release(); } catch (Exception ignored) {}
        }
        binding = null;
    }
}
