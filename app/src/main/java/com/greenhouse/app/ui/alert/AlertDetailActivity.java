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
        binding.tvSensorType.setText(sensorTypeLabel(sensorType));
        binding.tvSensorValue.setText(String.valueOf(sensorValue));
        binding.tvTime.setText(formatTime(time));
    }

    /** 传感器类型 → 中文（与后端枚举一致） */
    private String sensorTypeLabel(String type) {
        if (type == null) return "--";
        switch (type) {
            case "TEMPERATURE": return "空气温度";
            case "HUMIDITY": return "空气湿度";
            case "LIGHT": return "光照强度";
            case "CO2": return "CO₂浓度";
            case "SOIL_TEMP": return "土壤温度";
            case "SOIL_MOISTURE": return "土壤湿度";
            case "SOIL_PH": return "土壤pH";
            case "WIND_SPEED": return "风速";
            default: return type;
        }
    }

    /** ISO-8601 时间 → 易读格式（2026-08-07T08:09:02 → 2026-08-07 08:09） */
    private String formatTime(String raw) {
        if (raw == null || raw.isEmpty()) return "--";
        String t = raw;
        int dotIdx = t.indexOf('.');
        if (dotIdx > 0) t = t.substring(0, dotIdx);
        if (t.length() >= 16) {
            String datePart = t.substring(0, 10);
            String timePart = t.substring(11, 16);
            return datePart + " " + timePart;
        }
        return t;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}