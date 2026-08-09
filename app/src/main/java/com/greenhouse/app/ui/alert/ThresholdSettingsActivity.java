package com.greenhouse.app.ui.alert;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.greenhouse.app.adapter.ThresholdAdapter;
import com.greenhouse.app.data.model.ThresholdItem;
import com.greenhouse.app.databinding.ActivityThresholdSettingsBinding;
import com.greenhouse.app.viewmodel.AlertViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义预警阈值设置页 (F02)
 */
public class ThresholdSettingsActivity extends AppCompatActivity {

    private ActivityThresholdSettingsBinding binding;
    private AlertViewModel viewModel;
    private ThresholdAdapter adapter;

    /** 默认传感器类型列表 */
    private static final String[] DEFAULT_SENSOR_TYPES = {
            "TEMP", "HUMIDITY", "LIGHT", "CO2", "O2",
            "SOIL_TEMP", "SOIL_HUMIDITY", "EC"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityThresholdSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(AlertViewModel.class);

        // RecyclerView
        adapter = new ThresholdAdapter();
        binding.rvThresholds.setLayoutManager(new LinearLayoutManager(this));
        binding.rvThresholds.setAdapter(adapter);

        // 初始化默认阈值列表
        viewModel.init(1); // 默认大棚ID=1

        // 观察已保存的阈值
        viewModel.getThresholds().observe(this, savedThresholds -> {
            // 合并默认类型和已保存值
            List<ThresholdItem> merged = mergeWithDefaults(savedThresholds);
            adapter.setData(merged);
        });

        // 观察操作消息
        viewModel.getThresholdMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // 保存按钮
        binding.btnSave.setOnClickListener(v -> {
            List<ThresholdItem> items = adapter.getItems();
            for (ThresholdItem item : items) {
                if (item.getMinThreshold() != null && item.getMaxThreshold() != null) {
                    viewModel.saveThreshold(item);
                }
            }
            Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 合并默认类型列表和已保存的阈值
     */
    private List<ThresholdItem> mergeWithDefaults(List<ThresholdItem> saved) {
        List<ThresholdItem> result = new ArrayList<>();

        for (String type : DEFAULT_SENSOR_TYPES) {
            ThresholdItem item = new ThresholdItem();
            item.setSensorType(type);

            // 查找已保存的值
            if (saved != null) {
                for (ThresholdItem s : saved) {
                    if (type.equals(s.getSensorType())) {
                        item.setId(s.getId());
                        item.setMinThreshold(s.getMinThreshold());
                        item.setMaxThreshold(s.getMaxThreshold());
                        break;
                    }
                }
            }

            result.add(item);
        }

        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}