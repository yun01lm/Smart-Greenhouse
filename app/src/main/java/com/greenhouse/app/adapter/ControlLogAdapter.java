package com.greenhouse.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.ControlLogItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备控制记录列表适配器
 * <p>
 * 每行展示：设备名 + 动作（开启/关闭）+ 来源标签（手动/场景触发/预警联动）
 * + 时间 + 操作人 + 结果 + 触发场景/失败原因。
 * </p>
 */
public class ControlLogAdapter extends RecyclerView.Adapter<ControlLogAdapter.LogViewHolder> {

    private final List<ControlLogItem> logs = new ArrayList<>();

    public void setData(List<ControlLogItem> newLogs) {
        logs.clear();
        if (newLogs != null) logs.addAll(newLogs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_control_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        holder.bind(logs.get(position));
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvDevice;
        TextView tvAction;
        TextView tvSource;
        TextView tvTime;
        TextView tvOperator;
        TextView tvResult;
        TextView tvExtra;

        LogViewHolder(View itemView) {
            super(itemView);
            tvDevice = itemView.findViewById(R.id.tv_log_device);
            tvAction = itemView.findViewById(R.id.tv_log_action);
            tvSource = itemView.findViewById(R.id.tv_log_source);
            tvTime = itemView.findViewById(R.id.tv_log_time);
            tvOperator = itemView.findViewById(R.id.tv_log_operator);
            tvResult = itemView.findViewById(R.id.tv_log_result);
            tvExtra = itemView.findViewById(R.id.tv_log_extra);
        }

        void bind(ControlLogItem item) {
            tvDevice.setText(item.getDeviceName() != null ? item.getDeviceName() : "未知设备");
            tvAction.setText("ON".equals(item.getAction()) ? "开启" : "关闭");
            tvSource.setText(sourceLabel(item.getSource()));
            tvTime.setText(formatTime(item.getCreatedAt()));
            tvOperator.setText("操作人：" + (item.getUsername() != null ? item.getUsername() : "系统"));

            boolean ok = item.getSuccess() == null || item.getSuccess();
            tvResult.setText(ok ? "成功" : "失败");
            tvResult.setTextColor(itemView.getContext().getColor(
                    ok ? R.color.confidence_high : R.color.alert_critical));

            String extra = null;
            if ("SCENE".equals(item.getSource()) || "ALERT".equals(item.getSource())) {
                if (item.getSceneName() != null) {
                    extra = ("ALERT".equals(item.getSource()) ? "预警联动触发场景：" : "触发场景：")
                            + item.getSceneName();
                }
            }
            if (!ok && item.getFailReason() != null && !item.getFailReason().isEmpty()) {
                extra = (extra != null ? extra + "\n" : "") + "失败原因：" + item.getFailReason();
            }
            tvExtra.setVisibility(extra != null ? View.VISIBLE : View.GONE);
            tvExtra.setText(extra);
        }

        private String sourceLabel(String source) {
            if ("SCENE".equals(source)) return "场景触发";
            if ("ALERT".equals(source)) return "预警联动";
            return "手动控制";
        }

        private String formatTime(String raw) {
            if (raw == null || raw.isEmpty()) return "";
            // 后端 LocalDateTime 形如 2026-08-14T00:12:34.123456，截断到分钟
            String t = raw.replace('T', ' ');
            int dot = t.indexOf('.');
            if (dot > 0) t = t.substring(0, dot);
            if (t.length() > 16) t = t.substring(0, 16);
            return t;
        }
    }
}
