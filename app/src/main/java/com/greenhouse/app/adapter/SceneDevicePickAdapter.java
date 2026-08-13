package com.greenhouse.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.DeviceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建场景对话框：设备动作选择适配器
 * <p>每行：勾选包含该设备，右侧开关选择动作（开=ON / 关=OFF）。</p>
 */
public class SceneDevicePickAdapter extends RecyclerView.Adapter<SceneDevicePickAdapter.PickViewHolder> {

    private final List<DeviceInfo> devices = new ArrayList<>();
    private final List<Boolean> checked = new ArrayList<>();
    private final List<Boolean> on = new ArrayList<>();

    public void setDevices(List<DeviceInfo> list) {
        devices.clear();
        checked.clear();
        on.clear();
        if (list != null) {
            for (DeviceInfo device : list) {
                devices.add(device);
                checked.add(false);
                on.add(true);
            }
        }
        notifyDataSetChanged();
    }

    public DeviceInfo getDevice(int position) {
        return devices.get(position);
    }

    public boolean isChecked(int position) {
        return checked.get(position);
    }

    public boolean isOn(int position) {
        return on.get(position);
    }

    @NonNull
    @Override
    public PickViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scene_device_pick, parent, false);
        return new PickViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PickViewHolder holder, int position) {
        holder.bind(devices.get(position), checked.get(position), on.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class PickViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbInclude;
        TextView tvName;
        TextView tvZone;
        Switch swAction;

        PickViewHolder(View itemView) {
            super(itemView);
            cbInclude = itemView.findViewById(R.id.cb_pick_include);
            tvName = itemView.findViewById(R.id.tv_pick_name);
            tvZone = itemView.findViewById(R.id.tv_pick_zone);
            swAction = itemView.findViewById(R.id.sw_pick_action);
        }

        void bind(DeviceInfo device, boolean checkedState, boolean onState) {
            tvName.setText(device.getName());
            tvZone.setText(device.getInstallLocation() != null ? device.getInstallLocation() : "");
            swAction.setText(onState ? "开" : "关");

            // 先清除旧监听再设置状态，避免复用触发误操作
            cbInclude.setOnCheckedChangeListener(null);
            cbInclude.setChecked(checkedState);
            cbInclude.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    checked.set(pos, isChecked);
                }
            });

            swAction.setOnCheckedChangeListener(null);
            swAction.setChecked(onState);
            swAction.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    on.set(pos, isChecked);
                    swAction.setText(isChecked ? "开" : "关");
                }
            });
        }
    }
}
