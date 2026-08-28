package com.greenhouse.app.ui.history;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.greenhouse.app.R;
import com.greenhouse.app.data.model.HistoryDataPoint;
import com.greenhouse.app.data.model.HistoryResponse;
import com.greenhouse.app.data.model.SensorDataPoint;
import com.greenhouse.app.data.repository.SensorRepository;
import com.greenhouse.app.databinding.ActivityHistoryBinding;
import com.greenhouse.app.viewmodel.HistoryViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 历史数据趋势图页面 (F06)
 * <p>
 * 展示传感器历史数据趋势曲线，支持传感器类型切换和时间范围选择。
 * 符合规范：Activity 只负责 UI，业务逻辑在 ViewModel。
 * </p>
 */
public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private HistoryViewModel viewModel;
    private ArrayAdapter<HistoryViewModel.SensorTypeItem> sensorTypeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        // 从 Intent 获取大棚ID
        long greenhouseId = getIntent().getLongExtra("greenhouse_id", 1);
        viewModel.setCurrentGreenhouseId(greenhouseId);

        // 传感器类型选择器
        sensorTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        sensorTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSensorType.setAdapter(sensorTypeAdapter);

        viewModel.getSensorTypes().observe(this, types -> {
            sensorTypeAdapter.clear();
            sensorTypeAdapter.addAll(types);
            // 默认选中第一个
            if (types != null && !types.isEmpty()) {
                binding.spinnerSensorType.setSelection(0);
            }
        });

        binding.spinnerSensorType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View v, int pos, long id) {
                HistoryViewModel.SensorTypeItem item = sensorTypeAdapter.getItem(pos);
                if (item != null) {
                    viewModel.selectSensorType(item.getCode());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 时间范围选择
        binding.chip1h.setOnClickListener(v -> selectTimeRange("1h"));
        binding.chip24h.setOnClickListener(v -> selectTimeRange("24h"));
        binding.chip7d.setOnClickListener(v -> selectTimeRange("7d"));
        binding.chip30d.setOnClickListener(v -> selectTimeRange("30d"));

        // 默认选中 24h
        selectTimeRange("24h");

        // 观察数据
        viewModel.getDataPoints().observe(this, this::updateChart);
        viewModel.getResponseMeta().observe(this, this::updateMeta);
        viewModel.getIsLoading().observe(this, loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ?
                        android.view.View.VISIBLE : android.view.View.GONE));
        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectTimeRange(String range) {
        // 重置所有 chip 样式
        binding.chip1h.setChecked(false);
        binding.chip24h.setChecked(false);
        binding.chip7d.setChecked(false);
        binding.chip30d.setChecked(false);

        switch (range) {
            case "1h": binding.chip1h.setChecked(true); break;
            case "24h": binding.chip24h.setChecked(true); break;
            case "7d": binding.chip7d.setChecked(true); break;
            case "30d": binding.chip30d.setChecked(true); break;
        }
        viewModel.selectTimeRange(range);
    }

    private void updateMeta(HistoryResponse meta) {
        if (meta == null) return;
        binding.tvChartTitle.setText(meta.getSensorTypeName());
        binding.tvChartUnit.setText(meta.getUnitText());
    }

    private void updateChart(List<HistoryDataPoint> points) {
        if (points == null || points.isEmpty()) {
            binding.lineChart.clear();
            binding.lineChart.setNoDataText("暂无历史数据");
            return;
        }

        List<Entry> avgEntries = new ArrayList<>();
        List<Entry> minEntries = new ArrayList<>();
        List<Entry> maxEntries = new ArrayList<>();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat labelFormat;
        String timeRange = viewModel.getSelectedTimeRange().getValue();
        if ("1h".equals(timeRange) || "24h".equals(timeRange)) {
            labelFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else {
            labelFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
        }

        List<String> xLabels = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            HistoryDataPoint p = points.get(i);
            float x = i;
            if (p.getAvg() != null) avgEntries.add(new Entry(x, p.getAvg().floatValue()));
            if (p.getMin() != null) minEntries.add(new Entry(x, p.getMin().floatValue()));
            if (p.getMax() != null) maxEntries.add(new Entry(x, p.getMax().floatValue()));

            // X轴标签
            try {
                Date date = inputFormat.parse(p.getTime());
                xLabels.add(date != null ? labelFormat.format(date) : "");
            } catch (Exception e) {
                xLabels.add("");
            }
        }

        // 平均值线（蓝色实线）
        LineDataSet avgSet = new LineDataSet(avgEntries, "平均值");
        avgSet.setColor(Color.parseColor("#2196F3"));
        avgSet.setLineWidth(2f);
        avgSet.setCircleRadius(1f);
        avgSet.setDrawCircleHole(false);
        avgSet.setDrawValues(false);
        avgSet.setMode(LineDataSet.Mode.LINEAR);

        // 最大值线（红色虚线）
        LineDataSet maxSet = new LineDataSet(maxEntries, "最大值");
        maxSet.setColor(Color.parseColor("#F44336"));
        maxSet.setLineWidth(1f);
        maxSet.setCircleRadius(0f);
        maxSet.setDrawValues(false);
        maxSet.enableDashedLine(10f, 5f, 0f);
        maxSet.setMode(LineDataSet.Mode.LINEAR);

        // 最小值线（绿色虚线）
        LineDataSet minSet = new LineDataSet(minEntries, "最小值");
        minSet.setColor(Color.parseColor("#4CAF50"));
        minSet.setLineWidth(1f);
        minSet.setCircleRadius(0f);
        minSet.setDrawValues(false);
        minSet.enableDashedLine(10f, 5f, 0f);
        minSet.setMode(LineDataSet.Mode.LINEAR);

        LineData lineData = new LineData(avgSet, maxSet, minSet);
        binding.lineChart.setData(lineData);

        // X轴配置
        XAxis xAxis = binding.lineChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < xLabels.size()) {
                    // 减少标签密度
                    int step = Math.max(1, xLabels.size() / 6);
                    return (index % step == 0) ? xLabels.get(index) : "";
                }
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);

        binding.lineChart.getAxisRight().setEnabled(false);
        binding.lineChart.getDescription().setEnabled(false);
        binding.lineChart.setTouchEnabled(true);
        binding.lineChart.setDragEnabled(true);
        binding.lineChart.setScaleEnabled(true);
        binding.lineChart.animateX(500);
        binding.lineChart.invalidate();

        // 深色主题适配：轴/网格/图例文字
        int axisText = Color.parseColor("#9DB0A6");
        int gridLine = Color.parseColor("#22312A");
        xAxis.setTextColor(axisText);
        xAxis.setGridColor(gridLine);
        binding.lineChart.getAxisLeft().setTextColor(axisText);
        binding.lineChart.getAxisLeft().setGridColor(gridLine);
        binding.lineChart.getAxisRight().setTextColor(axisText);
        binding.lineChart.getLegend().setTextColor(axisText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export) {
            exportCsv();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** 导出当前传感器近 24h 数据为 CSV 并分享（F4） */
    private void exportCsv() {
        String sensorType = viewModel.getSelectedSensorType().getValue();
        if (sensorType == null) {
            Toast.makeText(this, "请先选择传感器类型", Toast.LENGTH_SHORT).show();
            return;
        }
        long ghId = getIntent().getLongExtra("greenhouse_id", 1);
        long end = System.currentTimeMillis();
        long start = end - 24L * 3600 * 1000;
        Toast.makeText(this, "正在导出...", Toast.LENGTH_SHORT).show();

        new SensorRepository().getHistory(ghId, sensorType, start, end, "1m",
                new SensorRepository.Callback<List<SensorDataPoint>>() {
                    @Override
                    public void onSuccess(List<SensorDataPoint> data) {
                        shareCsv(sensorType, data);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(HistoryActivity.this, "导出失败: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void shareCsv(String sensorType, List<SensorDataPoint> data) {
        StringBuilder sb = new StringBuilder("时间,数值\n");
        if (data != null) {
            for (SensorDataPoint p : data) {
                sb.append(p.getTimestamp() == null ? "" : p.getTimestamp()).append(',')
                        .append(p.getValue()).append('\n');
            }
        }
        try {
            File dir = new File(getCacheDir(), "export");
            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(this, "创建导出目录失败", Toast.LENGTH_SHORT).show();
                return;
            }
            File file = new File(dir, "sensor_" + sensorType + "_" + System.currentTimeMillis() + ".csv");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(sb.toString().getBytes("UTF-8"));
            }
            android.net.Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "分享 CSV 数据"));
        } catch (IOException e) {
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
