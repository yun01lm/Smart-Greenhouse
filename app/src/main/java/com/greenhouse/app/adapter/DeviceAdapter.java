package com.greenhouse.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备控制列表适配器
 * <p>
 * 每个设备卡片展示：类型图标 + 名称 + 安装位置 + 状态 + 开关按钮。
 * 数据来自后端 DeviceResponse（控制器类设备）。
 * </p>
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final List<DeviceInfo> devices = new ArrayList<>();
    private OnDeviceSwitchListener listener;

    public interface OnDeviceSwitchListener {
        void onSwitchChanged(DeviceInfo device, boolean turnOn);
    }

    public void setOnDeviceSwitchListener(OnDeviceSwitchListener listener) {
        this.listener = listener;
    }

    public void setData(List<DeviceInfo> newDevices) {
        devices.clear();
        if (newDevices != null) devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_control, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.bind(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvZone;
        TextView tvStatus;
        Switch swControl;

        DeviceViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_device_icon);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvZone = itemView.findViewById(R.id.tv_device_zone);
            tvStatus = itemView.findViewById(R.id.tv_device_status);
            swControl = itemView.findViewById(R.id.sw_control);
        }

        void bind(DeviceInfo device) {
            ivIcon.setImageResource(resolveIconRes(device.getName()));
            tvName.setText(device.getName());
            tvZone.setText(device.getInstallLocation() != null ? device.getInstallLocation() : "");

            // 在线状态：仅 OFFLINE 视为离线，其余（ONLINE/ALARM）可控制
            if (!device.isOnline()) {
                tvStatus.setText("离线");
                tvStatus.setTextColor(itemView.getContext().getResources()
                        .getColor(R.color.alert_critical, null));
                swControl.setEnabled(false);
                swControl.setChecked(false);
            } else {
                boolean running = device.isRunning();
                tvStatus.setText(running ? "运行中" : "已停止");
                tvStatus.setTextColor(itemView.getContext().getResources()
                        .getColor(running ? R.color.confidence_high : R.color.text_secondary, null));
                swControl.setEnabled(true);
                swControl.setChecked(running);
            }

            // 开关监听（避免 setChecked 触发无限循环）
            swControl.setOnCheckedChangeListener(null);
            swControl.setChecked(device.isOnline() && device.isRunning());
            swControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null && device.isOnline()) {
                    listener.onSwitchChanged(device, isChecked);
                }
            });
        }

        /**
         * 按设备名称关键字推断控制器图标（后端只有 CONTROLLER 大类，无细分类型）
         */
        private int resolveIconRes(String name) {
            if (name == null) return R.drawable.ic_device_default;
            if (name.contains("灯")) return R.drawable.ic_device_light;
            if (name.contains("风")) return R.drawable.ic_device_fan;
            if (name.contains("卷帘") || name.contains("遮阳")) return R.drawable.ic_device_roller;
            if (name.contains("泵") || name.contains("阀") || name.contains("灌溉")) return R.drawable.ic_device_valve;
            return R.drawable.ic_device_default;
        }
    }
}