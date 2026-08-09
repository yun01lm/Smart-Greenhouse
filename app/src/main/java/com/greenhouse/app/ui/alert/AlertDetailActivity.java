package com.greenhouse.app.ui.alert;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.greenhouse.app.databinding.ActivityAlertDetailBinding;

/**
 * 预警详情页 (F02)
 */
public class AlertDetailActivity extends AppCompatActivity {

    private ActivityAlertDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlertDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // 从 Intent 获取数据
        String title = getIntent().getStringExtra("alert_title");
        String content = getIntent().getStringExtra("alert_content");
        String level = getIntent().getStringExtra("alert_level");
        String time = getIntent().getStringExtra("alert_time");
        String sensorType = getIntent().getStringExtra("sensor_type");
        double sensorValue = getIntent().getDoubleExtra("sensor_value", 0);

        // 级别标签
        int levelColor;
        String levelText;
        if ("CRITICAL".equals(level)) {
            levelColor = Color.parseColor("#F44336");
            levelText = "严重预警";
        } else if ("WARNING".equals(level)) {
            levelColor = Color.parseColor("#FF9800");
            levelText = "警告预警";
        } else {
            levelColor = Color.parseColor("#2196F3");
            levelText = "提示信息";
        }

        binding.tvLevelBadge.setText(levelText);
        binding.tvLevelBadge.getBackground().setTint(levelColor);

        // 填充数据
        binding.tvTitle.setText(title != null ? title : "");
        binding.tvContent.setText(content != null ? content : "");
        binding.tvSensorType.setText(sensorType != null ? sensorType : "--");
        binding.tvSensorValue.setText(String.valueOf(sensorValue));
        binding.tvTime.setText(time != null ? time : "--");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}