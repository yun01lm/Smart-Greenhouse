package com.greenhouse.app.ui.growth;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.greenhouse.app.data.model.CropCycleData;
import com.greenhouse.app.data.model.GrowthAssessment;
import com.greenhouse.app.data.model.GrowthImage;
import com.greenhouse.app.databinding.ActivityGrowthBinding;
import com.greenhouse.app.viewmodel.GrowthViewModel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * 作物长势评估页面 (F07)
 * <p>
 * 展示摄像头截帧图片、AI识别的生长阶段、株高/叶面积/叶色等长势特征，
 * 以及当前种植周期信息。符合规范：Activity 只负责 UI，业务逻辑在 ViewModel。
 * </p>
 */
public class GrowthActivity extends AppCompatActivity {

    private ActivityGrowthBinding binding;
    private GrowthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGrowthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(GrowthViewModel.class);

        // 从 Intent 获取大棚ID
        long greenhouseId = getIntent().getLongExtra("greenhouse_id", 1);
        viewModel.setCurrentGreenhouseId(greenhouseId);

        // 观察最新长势评估
        viewModel.getLatestAssessment().observe(this, this::updateAssessment);

        // 观察截帧图片列表
        viewModel.getImageList().observe(this, this::updateImages);

        // 观察种植周期
        viewModel.getActiveCropCycle().observe(this, this::updateCropCycle);

        // 观察加载状态
        viewModel.getIsLoading().observe(this, loading ->
                binding.progressBar.setVisibility(
                        Boolean.TRUE.equals(loading) ? android.view.View.VISIBLE : android.view.View.GONE));

        // 观察错误
        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        // 加载全部数据
        viewModel.loadAll();
    }

    /**
     * 更新长势评估展示
     */
    private void updateAssessment(GrowthAssessment assessment) {
        if (assessment == null) {
            binding.layoutAssessment.setVisibility(android.view.View.GONE);
            binding.tvNoAssessment.setVisibility(android.view.View.VISIBLE);
            return;
        }

        binding.layoutAssessment.setVisibility(android.view.View.VISIBLE);
        binding.tvNoAssessment.setVisibility(android.view.View.GONE);

        // 生长阶段
        binding.tvGrowthStage.setText(assessment.getGrowthStageText());
        binding.tvGrowthStage.setTextColor(assessment.getHealthLevelColor());

        // 健康评分
        binding.tvHealthScore.setText(assessment.getHealthScoreText());
        binding.tvHealthScore.getBackground().setTint(assessment.getHealthLevelColor());
        binding.tvHealthLevel.setText(assessment.getHealthLevel());
        binding.tvHealthLevel.setTextColor(assessment.getHealthLevelColor());

        // 株高
        binding.tvPlantHeight.setText(assessment.getPlantHeightText());

        // 叶面积
        binding.tvLeafArea.setText(assessment.getLeafAreaText());

        // 叶色
        binding.tvLeafColor.setText(assessment.getLeafColorText());

        // 评估时间
        if (assessment.getCreatedAt() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                binding.tvAssessmentTime.setText("评估时间：" + outputFormat.format(
                        inputFormat.parse(assessment.getCreatedAt())));
            } catch (Exception e) {
                binding.tvAssessmentTime.setText("评估时间：" + assessment.getCreatedAt());
            }
        }
    }

    /**
     * 更新截帧图片信息
     */
    private void updateImages(List<GrowthImage> images) {
        if (images == null || images.isEmpty()) {
            binding.tvImageCount.setText("暂无截帧图片");
            return;
        }

        GrowthImage latest = images.get(0);
        binding.tvImageCount.setText("截帧图片共 " + images.size() + " 张（每30分钟）");

        // 最新截帧信息
        if (latest.getCapturedAt() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                binding.tvLatestCapture.setText("最新截帧：" + outputFormat.format(
                        inputFormat.parse(latest.getCapturedAt())));
            } catch (Exception e) {
                binding.tvLatestCapture.setText("最新截帧：" + latest.getCapturedAt());
            }
        }

        binding.tvResolution.setText(latest.getResolution() != null ? latest.getResolution() : "--");
        binding.tvFileSize.setText(latest.getFileSizeText());
    }

    /**
     * 更新种植周期信息
     */
    private void updateCropCycle(CropCycleData cycle) {
        if (cycle == null) {
            binding.layoutCropCycle.setVisibility(android.view.View.GONE);
            return;
        }

        binding.layoutCropCycle.setVisibility(android.view.View.VISIBLE);

        // 作物名称 + 品种
        String cropName = (cycle.getCropType() != null ? cycle.getCropType() : "--")
                + (cycle.getVariety() != null && !cycle.getVariety().isEmpty()
                        ? "（" + cycle.getVariety() + "）" : "");
        binding.tvCropName.setText(cropName);

        // 种植日期
        binding.tvPlantingDate.setText(cycle.getPlantingDate() != null ? cycle.getPlantingDate() : "--");

        // 已种植天数
        binding.tvDaysGrown.setText("已种植 " + cycle.getDaysSincePlanting() + " 天");

        // 当前阶段
        binding.tvCurrentStage.setText(cycle.getCurrentStage() != null ? cycle.getCurrentStage() : "--");

        // 阶段来源
        String sourceText = "AUTO".equals(cycle.getStageSource()) ? "（自动估算）" : "（手动设置）";
        binding.tvStageSource.setText(sourceText);

        // 状态
        String statusText;
        int statusColor;
        switch (cycle.getStatus() != null ? cycle.getStatus() : "") {
            case "ACTIVE":
                statusText = "● 进行中";
                statusColor = 0xFF4CAF50;
                break;
            case "COMPLETED":
                statusText = "● 已收获";
                statusColor = 0xFF2196F3;
                break;
            case "CANCELLED":
                statusText = "● 已取消";
                statusColor = 0xFF9E9E9E;
                break;
            default:
                statusText = "● " + cycle.getStatus();
                statusColor = 0xFF757575;
        }
        binding.tvCropStatus.setText(statusText);
        binding.tvCropStatus.setTextColor(statusColor);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
