package com.greenhouse.app.adapter;

import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.ThresholdItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阈值编辑适配器 (F02)
 */
public class ThresholdAdapter extends RecyclerView.Adapter<ThresholdAdapter.ViewHolder> {

    private final List<ThresholdItem> items = new ArrayList<>();

    /** 传感器类型中文名映射（键与后端 SensorType 枚举一致） */
    private static final Map<String, String> NAME_MAP = new HashMap<>();
    static {
        NAME_MAP.put("TEMPERATURE", "空气温度 (°C)");
        NAME_MAP.put("HUMIDITY", "空气湿度 (%)");
        NAME_MAP.put("LIGHT", "光照强度 (lux)");
        NAME_MAP.put("CO2", "CO₂浓度 (ppm)");
        NAME_MAP.put("SOIL_TEMP", "土壤温度 (°C)");
        NAME_MAP.put("SOIL_MOISTURE", "土壤湿度 (%)");
        NAME_MAP.put("SOIL_PH", "土壤pH");
        NAME_MAP.put("WIND_SPEED", "风速 (m/s)");
    }

    public void setData(List<ThresholdItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<ThresholdItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_threshold_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThresholdItem item = items.get(position);

        String name = NAME_MAP.getOrDefault(item.getSensorType(), item.getSensorType());
        holder.tvName.setText(name);

        if (item.getMinThreshold() != null) {
            holder.etMin.setText(String.valueOf(item.getMinThreshold()));
        }
        if (item.getMaxThreshold() != null) {
            holder.etMax.setText(String.valueOf(item.getMaxThreshold()));
        }

        // 监听输入变化更新模型
        holder.etMin.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    item.setMinThreshold(Double.parseDouble(holder.etMin.getText().toString()));
                } catch (NumberFormatException ignored) {}
            }
        });
        holder.etMax.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                try {
                    item.setMaxThreshold(Double.parseDouble(holder.etMax.getText().toString()));
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        EditText etMin, etMax;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_sensor_name);
            etMin = itemView.findViewById(R.id.et_min);
            etMax = itemView.findViewById(R.id.et_max);
        }
    }
}
