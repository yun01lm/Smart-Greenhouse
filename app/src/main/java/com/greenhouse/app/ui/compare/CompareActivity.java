package com.greenhouse.app.ui.compare;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.Greenhouse;
import com.greenhouse.app.data.model.SensorRealtimeData;
import com.greenhouse.app.data.repository.SensorRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多棚对比（第 2 项）
 * <p>勾选 2-3 个大棚 → 拉各棚实时数据 → 按传感器类型并列对比表格。</p>
 */
public class CompareActivity extends AppCompatActivity {

    private LinearLayout layoutGhPick;
    private LinearLayout layoutResult;
    private TextView tvResultTitle;
    private final List<Greenhouse> greenhouses = new ArrayList<>();
    private final List<CheckBox> checkBoxes = new ArrayList<>();

    private static final String[] SENSOR_KEYS = {"TEMPERATURE", "HUMIDITY", "CO2", "LIGHT", "SOIL_TEMP", "SOIL_MOISTURE"};
    private static final Map<String, String> SENSOR_CN = new LinkedHashMap<>();
    static {
        SENSOR_CN.put("TEMPERATURE", "空气温度 (°C)");
        SENSOR_CN.put("HUMIDITY", "空气湿度 (%)");
        SENSOR_CN.put("CO2", "CO₂浓度 (ppm)");
        SENSOR_CN.put("LIGHT", "光照强度 (lux)");
        SENSOR_CN.put("SOIL_TEMP", "土壤温度 (°C)");
        SENSOR_CN.put("SOIL_MOISTURE", "土壤湿度 (%)");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        layoutGhPick = findViewById(R.id.layout_gh_pick);
        layoutResult = findViewById(R.id.layout_result);
        tvResultTitle = findViewById(R.id.tv_result_title);
        findViewById(R.id.btn_compare).setOnClickListener(v -> doCompare());

        loadGreenhouses();
    }

    private void loadGreenhouses() {
        new SensorRepository().getGreenhouses(new SensorRepository.Callback<List<Greenhouse>>() {
            @Override
            public void onSuccess(List<Greenhouse> data) {
                greenhouses.clear();
                if (data != null) greenhouses.addAll(data);
                layoutGhPick.removeAllViews();
                checkBoxes.clear();
                for (Greenhouse gh : greenhouses) {
                    CheckBox cb = new CheckBox(CompareActivity.this);
                    cb.setText(gh.getName());
                    cb.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
                    layoutGhPick.addView(cb);
                    checkBoxes.add(cb);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CompareActivity.this, "加载大棚失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doCompare() {
        List<Greenhouse> picked = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isChecked()) picked.add(greenhouses.get(i));
        }
        if (picked.size() < 2) {
            Toast.makeText(this, "请至少选择 2 个大棚", Toast.LENGTH_SHORT).show();
            return;
        }
        if (picked.size() > 3) {
            Toast.makeText(this, "最多选择 3 个大棚", Toast.LENGTH_SHORT).show();
            return;
        }

        final int[] remaining = {picked.size()};
        final Map<Long, Map<String, Double>> allData = new LinkedHashMap<>();
        tvResultTitle.setVisibility(View.VISIBLE);
        tvResultTitle.setText("对比中...");

        for (Greenhouse gh : picked) {
            new SensorRepository().getRealtimeData(gh.getId(),
                    new SensorRepository.Callback<SensorRealtimeData>() {
                        @Override
                        public void onSuccess(SensorRealtimeData data) {
                            Map<String, Double> latest = new LinkedHashMap<>();
                            if (data.getDataByType() != null) {
                                for (Map.Entry<String, List<com.greenhouse.app.data.model.SensorDataPoint>> e
                                        : data.getDataByType().entrySet()) {
                                    if (e.getValue() != null && !e.getValue().isEmpty()) {
                                        latest.put(e.getKey(), e.getValue().get(0).getValue());
                                    }
                                }
                            }
                            allData.put(gh.getId(), latest);
                            synchronized (remaining) {
                                remaining[0]--;
                                if (remaining[0] == 0) runOnUiThread(() -> renderResult(picked, allData));
                            }
                        }

                        @Override
                        public void onError(String message) {
                            allData.put(gh.getId(), new LinkedHashMap<>());
                            synchronized (remaining) {
                                remaining[0]--;
                                if (remaining[0] == 0) runOnUiThread(() -> renderResult(picked, allData));
                            }
                        }
                    });
        }
    }

    private void renderResult(List<Greenhouse> picked, Map<Long, Map<String, Double>> allData) {
        layoutResult.removeAllViews();
        tvResultTitle.setText("实时数据对比");

        // 表头：空 + 每棚名
        addRow(true, "指标", picked, allData, null);

        // 每传感器一行
        for (String key : SENSOR_KEYS) {
            addRow(false, SENSOR_CN.getOrDefault(key, key), picked, allData, key);
        }
    }

    private void addRow(boolean header, String label, List<Greenhouse> picked,
                        Map<Long, Map<String, Double>> allData, String sensorKey) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(12, 10, 12, 10);
        row.setBackgroundColor(header ? 0x14FFFFFF : 0x08FFFFFF);

        // 指标名
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        tvLabel.setTextSize(13f);
        tvLabel.setWidth(300);
        row.addView(tvLabel);

        // 每棚数值
        for (Greenhouse gh : picked) {
            TextView tvVal = new TextView(this);
            Map<String, Double> data = allData.get(gh.getId());
            Double v = data != null && sensorKey != null ? data.get(sensorKey) : null;
            tvVal.setText(v != null ? String.format(java.util.Locale.CHINA, "%.1f", v) : "--");
            tvVal.setTextColor(getResources().getColor(
                    header ? R.color.text_secondary : R.color.text_primary, getTheme()));
            tvVal.setTextSize(header ? 12f : 15f);
            tvVal.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvVal.setGravity(android.view.Gravity.CENTER);
            tvVal.setWidth(280);
            row.addView(tvVal);
        }
        layoutResult.addView(row);
    }
}
