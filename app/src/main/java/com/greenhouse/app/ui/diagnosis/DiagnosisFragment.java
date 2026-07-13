package com.greenhouse.app.ui.diagnosis;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.adapter.DiagnosisHistoryAdapter;
import com.greenhouse.app.data.local.TokenManager;
import com.greenhouse.app.data.model.DiagnosisHistoryItem;
import com.greenhouse.app.data.model.DiagnosisResponse;
import com.greenhouse.app.databinding.FragmentDiagnosisBinding;
import com.greenhouse.app.viewmodel.DiagnosisViewModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 病虫害诊断主页
 * <p>
 * 符合规范：Fragment 只负责 UI 交互和导航，业务逻辑在 ViewModel。
 * </p>
 */
public class DiagnosisFragment extends Fragment {

    private FragmentDiagnosisBinding binding;
    private DiagnosisViewModel viewModel;
    private DiagnosisHistoryAdapter adapter;

    private Uri currentPhotoUri;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDiagnosisBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DiagnosisViewModel.class);

        // 获取 API 基础 URL
        String baseUrl = "http://10.0.2.2:8080";

        // 初始化 RecyclerView
        adapter = new DiagnosisHistoryAdapter(baseUrl);
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(adapter);

        // 注册 Activity Result（符合规范：使用 AndroidX Activity Result API）
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && currentPhotoUri != null) {
                        compressAndDiagnose(currentPhotoUri);
                    }
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        compressAndDiagnose(uri);
                    }
                });

        // 按钮事件
        binding.btnCamera.setOnClickListener(v -> takePhoto());
        binding.btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // 历史记录点击
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(requireContext(), DiagnosisResultActivity.class);
            intent.putExtra("diagnosis_id", item.getId());
            intent.putExtra("image_path", item.getImagePath());
            intent.putExtra("disease_name", item.getDiseaseName());
            intent.putExtra("confidence", item.getConfidence());
            intent.putExtra("treatment", item.getTreatment());
            intent.putExtra("recognition_engine", item.getRecognitionEngine());
            intent.putExtra("need_expert", item.getNeedExpert());
            intent.putExtra("created_at", item.getCreatedAt());
            intent.putExtra("base_url", baseUrl);
            startActivity(intent);
        });

        // 观察数据
        observeViewModel();
    }

    private void observeViewModel() {
        // 诊断结果 → 跳转结果页
        viewModel.getDiagnosisResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                String baseUrl = "http://10.0.2.2:8080";

                Intent intent = new Intent(requireContext(), DiagnosisResultActivity.class);
                intent.putExtra("diagnosis_id", result.getId());
                intent.putExtra("image_path", result.getImagePath());
                intent.putExtra("disease_name", result.getDiseaseName());
                intent.putExtra("confidence", result.getConfidence());
                intent.putExtra("treatment", result.getTreatment());
                intent.putExtra("recognition_engine", result.getRecognitionEngine());
                intent.putExtra("need_expert", result.getNeedExpert());
                intent.putExtra("created_at", result.getCreatedAt());
                intent.putExtra("base_url", baseUrl);
                startActivity(intent);
            }
        });

        // 历史列表
        viewModel.getHistoryList().observe(getViewLifecycleOwner(), list -> {
            adapter.setItems(list);
            if (list == null || list.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.rvHistory.setVisibility(View.GONE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.rvHistory.setVisibility(View.VISIBLE);
                binding.tvRecordCount.setText("共 " + list.size() + " 条记录");
            }
        });

        // 加载状态
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        // 错误
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== 拍照 =====

    private void takePhoto() {
        try {
            File photoFile = File.createTempFile("diagnosis_", ".jpg",
                    requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            currentPhotoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", photoFile);
            takePhotoLauncher.launch(currentPhotoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "创建图片文件失败", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== 图片压缩 =====

    private void compressAndDiagnose(Uri uri) {
        binding.progressBar.setVisibility(View.VISIBLE);

        // 在后台线程压缩图片（符合规范：不在主线程 IO）
        new Thread(() -> {
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                if (is == null) {
                    requireActivity().runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "读取图片失败", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 计算缩放比例
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, options);
                is.close();

                int maxWidth = 1024;
                int scale = 1;
                if (options.outWidth > maxWidth) {
                    scale = Math.round((float) options.outWidth / maxWidth);
                }

                // 重新读取并缩放
                is = requireContext().getContentResolver().openInputStream(uri);
                BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
                decodeOpts.inSampleSize = scale;
                Bitmap bitmap = BitmapFactory.decodeStream(is, null, decodeOpts);
                is.close();

                if (bitmap == null) {
                    requireActivity().runOnUiThread(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "图片解码失败", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // JPEG 压缩 80%
                File tempFile = File.createTempFile("diagnosis_compressed_", ".jpg",
                        requireContext().getCacheDir());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.write(baos.toByteArray());
                fos.close();
                baos.close();
                bitmap.recycle();

                // 上传诊断（在 Repository 的后台线程中执行）
                long greenhouseId = viewModel.getCurrentGreenhouseId();
                if (greenhouseId == 0) greenhouseId = 1; // 默认大棚
                viewModel.diagnose(tempFile, greenhouseId);

            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "图片处理失败: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ===== 生命周期 =====

    /**
     * 设置当前大棚ID（由 MainActivity 或 DashboardViewModel 提供）
     */
    public void setGreenhouseId(long greenhouseId) {
        if (viewModel != null) {
            viewModel.setCurrentGreenhouseId(greenhouseId);
            viewModel.loadHistory(greenhouseId);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次返回页面时刷新历史
        if (viewModel != null && viewModel.getCurrentGreenhouseId() > 0) {
            viewModel.loadHistory(viewModel.getCurrentGreenhouseId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
