package com.greenhouse.app.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.SensorDataPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传感器数据适配器
 * <p>
 * 展示各传感器类型的最新值和设备名称。
 * </p>
 */
public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.ViewHolder> {

    private final List<SensorDataPoint> items = new ArrayList<>();

    /** 传感器类型中文名映射（键与后端 SensorType 枚举一致） */
    private static final Map<String, String> NAME_MAP = new HashMap<>();
    private static final Map<String, String> UNIT_MAP = new HashMap<>();

    static {
        NAME_MAP.put("TEMPERATURE", "空气温度");  UNIT_MAP.put("TEMPERATURE", "°C");
        NAME_MAP.put("HUMIDITY", "空气湿度");   UNIT_MAP.put("HUMIDITY", "%");
        NAME_MAP.put("LIGHT", "光照强度");      UNIT_MAP.put("LIGHT", "lux");
        NAME_MAP.put("CO2", "CO₂浓度");        UNIT_MAP.put("CO2", "ppm");
        NAME_MAP.put("SOIL_TEMP", "土壤温度");   UNIT_MAP.put("SOIL_TEMP", "°C");
        NAME_MAP.put("SOIL_MOISTURE", "土壤湿度"); UNIT_MAP.put("SOIL_MOISTURE", "%");
        NAME_MAP.put("SOIL_PH", "土壤pH");      UNIT_MAP.put("SOIL_PH", "pH");
        NAME_MAP.put("WIND_SPEED", "风速");     UNIT_MAP.put("WIND_SPEED", "m/s");
    }

    public void setData(List<SensorDataPoint> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sensor_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SensorDataPoint point = items.get(position);

        String name = NAME_MAP.getOrDefault(point.getSensorType(), point.getSensorType());
        String unit = UNIT_MAP.getOrDefault(point.getSensorType(), "");

        holder.tvName.setText(name);
        holder.tvDevice.setText(point.getDeviceName());
        holder.tvValue.setText(formatValue(point.getValue()));
        holder.tvUnit.setText(unit);

        // 值颜色：正常绿色 / 异常红色（简化判断）
        boolean abnormal = isAbnormal(point.getSensorType(), point.getValue());
        holder.tvValue.setTextColor(abnormal ? Color.parseColor("#F44336") : Color.parseColor("#4CAF50"));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatValue(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    /** 简单异常判断（后续可用后端返回的阈值比对） */
    private boolean isAbnormal(String sensorType, double value) {
        switch (sensorType) {
            case "TEMPERATURE": return value < 15 || value > 35;
            case "HUMIDITY": return value < 40 || value > 90;
            case "CO2": return value < 300 || value > 1500;
            default: return false;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDevice, tvValue, tvUnit;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_sensor_name);
            tvDevice = itemView.findViewById(R.id.tv_sensor_device);
            tvValue = itemView.findViewById(R.id.tv_sensor_value);
            tvUnit = itemView.findViewById(R.id.tv_sensor_unit);
        }
    }
}
