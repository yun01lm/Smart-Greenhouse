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
import com.greenhouse.app.data.model.DeviceGroup;
import com.greenhouse.app.data.model.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备控制列表适配器（按大棚分组）
 * <p>
 * 支持两种 ViewType：分组标题（大棚名 + 设备数）与设备项（图标 + 名称 + 位置 + 状态 + 开关）。
 * </p>
 */
public class DeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_DEVICE = 1;

    private final List<Object> items = new ArrayList<>();
    private OnDeviceSwitchListener listener;

    public interface OnDeviceSwitchListener {
        void onSwitchChanged(DeviceInfo device, boolean turnOn);
    }

    public void setOnDeviceSwitchListener(OnDeviceSwitchListener listener) {
        this.listener = listener;
    }

    public void setGroups(List<DeviceGroup> groups) {
        items.clear();
        if (groups != null) {
            for (DeviceGroup group : groups) {
                items.add(group);
                if (group.getDevices() != null) {
                    items.addAll(group.getDevices());
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof DeviceGroup ? TYPE_HEADER : TYPE_DEVICE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_device_group_header, parent, false));
        }
        return new DeviceViewHolder(inflater.inflate(R.layout.item_device_control, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((DeviceGroup) items.get(position));
        } else {
            ((DeviceViewHolder) holder).bind((DeviceInfo) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ===== 分组头 =====

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupName;
        TextView tvGroupCount;

        HeaderViewHolder(View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tv_group_name);
            tvGroupCount = itemView.findViewById(R.id.tv_group_count);
        }

        void bind(DeviceGroup group) {
            tvGroupName.setText(group.getGreenhouseName() != null ? group.getGreenhouseName() : "大棚");
            tvGroupCount.setText(group.getDeviceCount() + " 台设备");
        }
    }

    // ===== 设备项 =====

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
            boolean online = "ONLINE".equals(device.getStatus());
            boolean isOn = "ON".equals(device.getLastValue());
            tvStatus.setText(online ? (isOn ? "运行中" : "已停止") : "离线");
            tvStatus.setTextColor(itemView.getContext().getColor(online ? R.color.primary : R.color.text_secondary));
            // 先清除旧监听再设置状态，避免 ViewHolder 复用时 setChecked 误触发上一次设备的控制指令
            swControl.setOnCheckedChangeListener(null);
            swControl.setChecked(isOn);
            swControl.setEnabled(online);
            swControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onSwitchChanged(device, isChecked);
                }
            });
        }

        int resolveIconRes(String name) {
            if (name == null) return R.drawable.ic_device_default;
            if (name.contains("风机") || name.contains("风扇")) return R.drawable.ic_device_fan;
            if (name.contains("灯")) return R.drawable.ic_device_light;
            if (name.contains("遮阳")) return R.drawable.ic_device_shade;
            if (name.contains("卷帘") || name.contains("卷膜")) return R.drawable.ic_device_roller;
            if (name.contains("灌溉") || name.contains("阀门") || name.contains("水")) return R.drawable.ic_device_valve;
            return R.drawable.ic_device_default;
        }
    }
}
