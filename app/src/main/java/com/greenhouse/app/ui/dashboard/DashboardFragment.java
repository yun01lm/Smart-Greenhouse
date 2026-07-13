package com.greenhouse.app.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.greenhouse.app.R;
import com.greenhouse.app.adapter.SensorAdapter;
import com.greenhouse.app.data.model.Greenhouse;
import com.greenhouse.app.data.model.HealthScoreData;
import com.greenhouse.app.databinding.FragmentDashboardBinding;
import com.greenhouse.app.viewmodel.DashboardViewModel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 实时数据看板 Fragment
 * <p>
 * 职责：UI 展示 + 事件绑定。业务逻辑全部在 DashboardViewModel 中。
 * 符合规范：Fragment 不写业务逻辑。
 * </p>
 */
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private SensorAdapter sensorAdapter;
    private ArrayAdapter<String> greenhouseAdapter;
    private List<Greenhouse> greenhouseList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // 初始化 RecyclerView
        sensorAdapter = new SensorAdapter();
        binding.rvSensors.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSensors.setAdapter(sensorAdapter);

        // 初始化大棚选择器
        greenhouseAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, new ArrayList<>());
        greenhouseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGreenhouse.setAdapter(greenhouseAdapter);

        binding.spinnerGreenhouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                if (pos < greenhouseList.size()) {
                    viewModel.selectGreenhouse(greenhouseList.get(pos).getId());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 观察大棚列表
        viewModel.getGreenhouses().observe(getViewLifecycleOwner(), greenhouses -> {
            greenhouseList = greenhouses != null ? greenhouses : new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Greenhouse g : greenhouseList) {
                names.add(g.getName());
            }
            greenhouseAdapter.clear();
            greenhouseAdapter.addAll(names);
            greenhouseAdapter.notifyDataSetChanged();
        });

        // 观察传感器数据
        viewModel.getDataPoints().observe(getViewLifecycleOwner(), points -> {
            sensorAdapter.setData(points);
        });

        // 观察健康评分
        viewModel.getHealthScore().observe(getViewLifecycleOwner(), this::updateHealthScore);

        // 观察加载状态
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
        });

        // 加载数据
        viewModel.loadGreenhouses();
    }

    private void updateHealthScore(HealthScoreData score) {
        if (score == null) return;

        binding.layoutHealthScore.setVisibility(View.VISIBLE);
        BigDecimal overall = score.getOverallScore();
        binding.tvHealthScore.setText(overall != null ? String.valueOf(overall.intValue()) : "--");

        // 根据等级设置颜色
        String color = score.getLevelColor();
        int bgColor = Color.parseColor("#4CAF50"); // 默认绿色
        if ("blue".equals(color)) bgColor = Color.parseColor("#2196F3");
        else if ("yellow".equals(color)) bgColor = Color.parseColor("#FFC107");
        else if ("orange".equals(color)) bgColor = Color.parseColor("#FF9800");
        else if ("red".equals(color)) bgColor = Color.parseColor("#F44336");

        binding.tvHealthScore.getBackground().setTint(bgColor);
        binding.tvHealthLevel.setText(score.getLevel());
        binding.tvHealthLevel.setTextColor(bgColor);

        String detail = String.format("环境 %.0f | 视觉 %.0f",
                score.getEnvScore() != null ? score.getEnvScore() : BigDecimal.ZERO,
                score.getVisualScore() != null ? score.getVisualScore() : BigDecimal.ZERO);
        binding.tvHealthDetail.setText(detail);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
