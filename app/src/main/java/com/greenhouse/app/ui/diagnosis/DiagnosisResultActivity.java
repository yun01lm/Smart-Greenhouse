package com.greenhouse.app.ui.diagnosis;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.greenhouse.app.R;
import com.greenhouse.app.databinding.ActivityDiagnosisResultBinding;

/**
 * 诊断结果页
 * <p>
 * 展示：作物图片 + 识别结果 + 置信度 + 防治方案 + 低置信度求助专家按钮
 * 符合规范：Activity 只负责 UI 展示和导航，不写业务逻辑。
 * </p>
 */
public class DiagnosisResultActivity extends AppCompatActivity {

    private ActivityDiagnosisResultBinding binding;

    private String imagePath;
    private String diseaseName;
    private double confidence;
    private String treatment;
    private String recognitionEngine;
    private boolean needExpert;
    private String createdAt;
    private String baseUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDiagnosisResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 读取 Intent 数据
        readIntentData();
        // 显示数据
        displayData();
    }

    private void readIntentData() {
        Intent intent = getIntent();
        imagePath = intent.getStringExtra("image_path");
        diseaseName = intent.getStringExtra("disease_name");
        confidence = intent.getDoubleExtra("confidence", 0.0);
        treatment = intent.getStringExtra("treatment");
        recognitionEngine = intent.getStringExtra("recognition_engine");
        needExpert = intent.getBooleanExtra("need_expert", false);
        createdAt = intent.getStringExtra("created_at");
        baseUrl = intent.getStringExtra("base_url");
        if (baseUrl == null) baseUrl = "http://10.0.2.2:8080";
    }

    private void displayData() {
        // 图片
        if (imagePath != null && !imagePath.isEmpty()) {
            String url = imagePath.startsWith("http") ? imagePath : baseUrl + imagePath;
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_camera)
                    .centerCrop()
                    .into(binding.ivDiagnosisImage);
        }

        // 病害名称
        binding.tvDiseaseName.setText(diseaseName != null ? diseaseName : "未知病害");

        // 置信度
        int confidencePercent = (int) Math.round(confidence * 100);
        String confidenceText = confidencePercent + "%";
        binding.tvConfidence.setText(confidenceText);
        binding.progressConfidence.setProgress(confidencePercent);

        // 置信度颜色
        int colorRes;
        if (confidence >= 0.80) {
            colorRes = R.color.confidence_high;
        } else if (confidence >= 0.70) {
            colorRes = R.color.confidence_medium;
        } else {
            colorRes = R.color.confidence_low;
        }
        int color = getResources().getColor(colorRes, getTheme());
        binding.tvConfidence.setTextColor(color);
        binding.progressConfidence.getProgressDrawable().setColorFilter(
                color, android.graphics.PorterDuff.Mode.SRC_IN);

        // 识别引擎
        String engineText = "识别引擎: " +
                (recognitionEngine != null ? recognitionEngine : "未知");
        binding.tvEngine.setText(engineText);

        // 时间
        binding.tvTime.setText(createdAt != null ? createdAt : "");

        // 防治方案
        binding.tvTreatment.setText(treatment != null ? treatment : "暂无防治方案");

        // 低置信度求助专家按钮
        if (needExpert) {
            binding.btnAskExpert.setVisibility(View.VISIBLE);
            binding.btnAskExpert.setOnClickListener(v -> {
                // F10 专家咨询开发后对接，当前提示
                Toast.makeText(this, "专家咨询功能开发中，敬请期待", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
