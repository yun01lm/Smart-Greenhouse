package com.greenhouse.app.ui.health;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.greenhouse.app.data.model.HealthScoreData;
import com.greenhouse.app.databinding.ActivityHealthBinding;
import com.greenhouse.app.viewmodel.HealthViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 多模态健康评分详情页 (F08)
 * <p>
 * 展示综合健康评分、子维度评分明细、历史趋势图、环境/长势分析、健康评估报告。
 * 符合规范：Activity 只负责 UI，业务逻辑在 ViewModel。
 * </p>
 */
public class HealthActivity extends AppCompatActivity {

    private ActivityHealthBinding binding;
    private HealthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHealthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(HealthViewModel.class);

        long greenhouseId = getIntent().getLongExtra("greenhouse_id", 1);
        viewModel.setCurrentGreenhouseId(greenhouseId);

        // 时间范围选择
        binding.chip7d.setOnClickListener(v -> {
            binding.chip7d.setChecked(true);
            binding.chip30d.setChecked(false);
            viewModel.selectTimeRange("7d");
        });
        binding.chip30d.setOnClickListener(v -> {
            binding.chip30d.setChecked(true);
            binding.chip7d.setChecked(false);
            viewModel.selectTimeRange("30d");
        });

        // 观察综合评分
        viewModel.getCurrentScore().observe(this, this::updateScoreDisplay);

        // 观察详情报告
        viewModel.getDetailReport().observe(this, this::updateDetailReport);

        // 观察历史趋势
        viewModel.getHistoryList().observe(this, this::updateChart);

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

        viewModel.loadAll();
    }

    /**
     * 更新综合评分展示
     */
    private void updateScoreDisplay(HealthScoreData score) {
        if (score == null) {
            binding.tvOverallScore.setText("--");
            binding.tvLevel.setText("暂无数据");
            return;
        }

        binding.tvOverallScore.setText(String.valueOf(score.getOverallScoreInt()));
        binding.tvOverallScore.setTextColor(score.getLevelColorInt());
        binding.tvLevel.setText(score.getLevel() != null ? score.getLevel() : "--");
        binding.tvLevel.setTextColor(score.getLevelColorInt());

        // 评分时间
        if (score.getCreatedAt() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                binding.tvScoreTime.setText("评估时间：" + outputFormat.format(
                        inputFormat.parse(score.getCreatedAt())));
            } catch (Exception e) {
                binding.tvScoreTime.setText("评估时间：" + score.getCreatedAt());
            }
        }

        // 子维度评分
        binding.tvEnvScore.setText(score.getEnvScoreText());
        binding.tvEnvScore.getBackground().setTint(scoreColor(score.getEnvScore()));

        binding.tvVisualScore.setText(score.getVisualScoreText());
        binding.tvVisualScore.getBackground().setTint(scoreColor(score.getVisualScore()));

        binding.tvWeatherRisk.setText(score.getWeatherRisk() != null ? score.getWeatherRisk() : "--");
        // 天气风险颜色
        int weatherColor;
        String risk = score.getWeatherRisk();
        if (risk == null) weatherColor = 0xFF9E9E9E;
        else if (risk.contains("低")) weatherColor = 0xFF4CAF50;
        else if (risk.contains("中")) weatherColor = 0xFFFFC107;
        else weatherColor = 0xFFF44336;
        binding.tvWeatherRisk.getBackground().setTint(weatherColor);
    }

    /**
     * 更新详情报告（环境分析 + 长势分析 + 报告）
     */
    private void updateDetailReport(HealthScoreData detail) {
        if (detail == null || !detail.hasAnalysisDetail()) {
            // 隐藏分析区域
            binding.layoutEnvAnalysis.setVisibility(android.view.View.GONE);
            binding.layoutVisualAnalysis.setVisibility(android.view.View.GONE);
            binding.layoutReport.setVisibility(android.view.View.GONE);
            return;
        }

        HealthScoreData.AnalysisDetail analysis = detail.getAnalysisJson();

        // ===== 环境健康度分析 =====
        if (analysis.getEnvDetail() != null) {
            binding.layoutEnvAnalysis.setVisibility(android.view.View.VISIBLE);
            binding.tvNoEnvAnalysis.setVisibility(android.view.View.GONE);

            HealthScoreData.AnalysisDetail.EnvDetail env = analysis.getEnvDetail();
            binding.tvEnvScoreLabel.setText("环境健康度：" + detail.getEnvScoreText() + " 分");

            setEnvItem(binding.tvTempScore, binding.tvTempComment, env.getTempScore(), env.getTempComment());
            setEnvItem(binding.tvHumidityScore, binding.tvHumidityComment, env.getHumidityScore(), env.getHumidityComment());
            setEnvItem(binding.tvCo2Score, binding.tvCo2Comment, env.getCo2Score(), env.getCo2Comment());
            setEnvItem(binding.tvSoilScore, binding.tvSoilComment, env.getSoilScore(), env.getSoilComment());
        } else {
            binding.layoutEnvAnalysis.setVisibility(android.view.View.GONE);
            binding.tvNoEnvAnalysis.setVisibility(android.view.View.VISIBLE);
        }

        // ===== 长势健康度分析 =====
        if (analysis.getVisualDetail() != null) {
            binding.layoutVisualAnalysis.setVisibility(android.view.View.VISIBLE);
            binding.tvNoVisualAnalysis.setVisibility(android.view.View.GONE);

            HealthScoreData.AnalysisDetail.VisualDetail visual = analysis.getVisualDetail();
            binding.tvVisualScoreLabel.setText("长势健康度：" + detail.getVisualScoreText() + " 分");

            setVisualItem(binding.tvLeafHealth, binding.tvLeafHealthComment,
                    visual.getLeafHealth(), visual.getLeafHealthComment());
            setVisualItem(binding.tvGrowthRate, binding.tvGrowthRateComment,
                    visual.getGrowthRate(), visual.getGrowthRateComment());
            setVisualItem(binding.tvDiseaseRisk, binding.tvDiseaseRiskComment,
                    visual.getDiseaseRisk(), visual.getDiseaseRiskComment());
        } else {
            binding.layoutVisualAnalysis.setVisibility(android.view.View.GONE);
            binding.tvNoVisualAnalysis.setVisibility(android.view.View.VISIBLE);
        }

        // ===== 健康评估报告 =====
        if (detail.hasRecommendations() || (analysis.getWeatherImpact() != null)) {
            binding.layoutReport.setVisibility(android.view.View.VISIBLE);
            binding.tvNoReport.setVisibility(android.view.View.GONE);

            // 天气影响
            if (analysis.getWeatherImpact() != null) {
                HealthScoreData.AnalysisDetail.WeatherImpact weather = analysis.getWeatherImpact();
                binding.tvCurrentWeather.setText("当前天气：" + (weather.getCurrentWeather() != null ? weather.getCurrentWeather() : "--"));
                binding.tvWeatherForecast.setText("预报：" + (weather.getForecast() != null ? weather.getForecast() : "--"));
                binding.tvRiskAssessment.setText(weather.getRiskAssessment() != null ? weather.getRiskAssessment() : "");
            }

            // 改善建议
            binding.tvRecommendations.setText(detail.getRecommendations() != null ? detail.getRecommendations() : "暂无建议");
        } else {
            binding.layoutReport.setVisibility(android.view.View.GONE);
            binding.tvNoReport.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void setEnvItem(android.widget.TextView scoreView, android.widget.TextView commentView,
                            Integer score, String comment) {
        if (score != null) {
            scoreView.setText(String.valueOf(score));
            scoreView.getBackground().setTint(scoreColor(score));
        } else {
            scoreView.setText("--");
        }
        commentView.setText(comment != null ? comment : "");
    }

    private void setVisualItem(android.widget.TextView scoreView, android.widget.TextView commentView,
                               Integer score, String comment) {
        if (score != null) {
            scoreView.setText(String.valueOf(score));
            scoreView.getBackground().setTint(scoreColor(score));
        } else {
            scoreView.setText("--");
        }
        commentView.setText(comment != null ? comment : "");
    }

    /**
     * 根据评分值返回颜色
     */
    private int scoreColor(Object scoreObj) {
        int score = 0;
        if (scoreObj instanceof Integer) score = (Integer) scoreObj;
        else if (scoreObj instanceof java.math.BigDecimal) score = ((java.math.BigDecimal) scoreObj).intValue();

        if (score >= 80) return 0xFF4CAF50;
        else if (score >= 60) return 0xFF2196F3;
        else if (score >= 40) return 0xFFFFC107;
        else return 0xFFF44336;
    }

    /**
     * 更新历史趋势图
     */
    private void updateChart(List<HealthScoreData> scores) {
        if (scores == null || scores.isEmpty()) {
            binding.chartHistory.clear();
            binding.chartHistory.setNoDataText("暂无历史评分数据");
            return;
        }

        List<Entry> overallEntries = new ArrayList<>();
        List<Entry> envEntries = new ArrayList<>();
        List<Entry> visualEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        // 反转列表使时间从旧到新
        for (int i = scores.size() - 1; i >= 0; i--) {
            HealthScoreData s = scores.get(i);
            int idx = scores.size() - 1 - i;
            float x = idx;

            if (s.getOverallScore() != null) {
                overallEntries.add(new Entry(x, s.getOverallScore().floatValue()));
            }
            if (s.getEnvScore() != null) {
                envEntries.add(new Entry(x, s.getEnvScore().floatValue()));
            }
            if (s.getVisualScore() != null) {
                visualEntries.add(new Entry(x, s.getVisualScore().floatValue()));
            }

            try {
                Date date = inputFormat.parse(s.getCreatedAt());
                xLabels.add(date != null ? labelFormat.format(date) : "");
            } catch (Exception e) {
                xLabels.add("");
            }
        }

        // 综合评分（绿色粗实线）
        LineDataSet overallSet = new LineDataSet(overallEntries, "综合评分");
        overallSet.setColor(Color.parseColor("#4CAF50"));
        overallSet.setLineWidth(2.5f);
        overallSet.setCircleRadius(2f);
        overallSet.setDrawCircleHole(false);
        overallSet.setDrawValues(false);
        overallSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // 环境健康度（蓝色虚线）
        LineDataSet envSet = new LineDataSet(envEntries, "环境健康");
        envSet.setColor(Color.parseColor("#2196F3"));
        envSet.setLineWidth(1.5f);
        envSet.setCircleRadius(0f);
        envSet.setDrawValues(false);
        envSet.enableDashedLine(8f, 4f, 0f);
        envSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // 长势健康度（橙色虚线）
        LineDataSet visualSet = new LineDataSet(visualEntries, "长势健康");
        visualSet.setColor(Color.parseColor("#FF9800"));
        visualSet.setLineWidth(1.5f);
        visualSet.setCircleRadius(0f);
        visualSet.setDrawValues(false);
        visualSet.enableDashedLine(8f, 4f, 0f);
        visualSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(overallSet, envSet, visualSet);
        binding.chartHistory.setData(lineData);

        // X轴
        XAxis xAxis = binding.chartHistory.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < xLabels.size()) {
                    int step = Math.max(1, xLabels.size() / 6);
                    return (index % step == 0) ? xLabels.get(index) : "";
                }
                return "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);

        binding.chartHistory.getAxisRight().setEnabled(false);
        binding.chartHistory.getAxisLeft().setAxisMinimum(0f);
        binding.chartHistory.getAxisLeft().setAxisMaximum(100f);
        binding.chartHistory.getDescription().setEnabled(false);
        binding.chartHistory.setTouchEnabled(true);
        binding.chartHistory.setDragEnabled(true);
        binding.chartHistory.setScaleEnabled(true);
        binding.chartHistory.animateX(500);
        binding.chartHistory.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
