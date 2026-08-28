package com.greenhouse.app.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.AlertItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 预警列表适配器 (F02)
 */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {

    private final List<AlertItem> items = new ArrayList<>();
    private OnItemClickListener listener;
    private OnHandleListener handleListener;

    public interface OnItemClickListener {
        void onItemClick(AlertItem alert);
    }

    /** 告警处理回调（第 4 项告警闭环） */
    public interface OnHandleListener {
        void onHandle(AlertItem alert);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnHandleListener(OnHandleListener listener) {
        this.handleListener = listener;
    }

    public void setData(List<AlertItem> newItems) {
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
                .inflate(R.layout.item_alert_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertItem alert = items.get(position);

        holder.tvTitle.setText(alert.getTitle());
        holder.tvContent.setText(alert.getContent());

        // 传感器信息
        String sensorInfo = alert.getSensorType() != null ? alert.getSensorType() : "";
        if (alert.getSensorValue() != null) {
            sensorInfo += " · 当前值: " + alert.getSensorValue();
        }
        holder.tvSensorInfo.setText(sensorInfo);

        // 时间格式化
        holder.tvTime.setText(formatTime(alert.getCreatedAt()));

        // 级别标签颜色
        String level = alert.getLevel();
        int levelColor;
        if ("CRITICAL".equals(level)) {
            levelColor = Color.parseColor("#F44336");
            holder.tvLevelTag.setText("严重");
        } else if ("WARNING".equals(level)) {
            levelColor = Color.parseColor("#FF9800");
            holder.tvLevelTag.setText("警告");
        } else {
            levelColor = Color.parseColor("#2196F3");
            holder.tvLevelTag.setText("提示");
        }
        holder.tvLevelTag.getBackground().setTint(levelColor);

        // 未读标记
        holder.dotUnread.setVisibility(alert.isReadStatus() ? View.GONE : View.VISIBLE);

        // 处理按钮（第 4 项）：已处理置灰，未处理可点击
        if (alert.isHandled()) {
            holder.tvHandle.setText("已处理");
            holder.tvHandle.setTextColor(Color.parseColor("#5C6F66"));
        } else {
            holder.tvHandle.setText("处理");
            holder.tvHandle.setTextColor(Color.parseColor("#3DDC84"));
            holder.tvHandle.setOnClickListener(v -> {
                if (handleListener != null) handleListener.onHandle(alert);
            });
        }

        // 点击
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(alert);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTime(String isoTime) {
        if (isoTime == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(isoTime.replace("Z", ""));
            if (date == null) return isoTime;

            long diff = System.currentTimeMillis() - date.getTime();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            long days = TimeUnit.MILLISECONDS.toDays(diff);

            if (minutes < 1) return "刚刚";
            if (minutes < 60) return minutes + "分钟前";
            if (hours < 24) return hours + "小时前";
            if (days < 7) return days + "天前";

            return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            return isoTime;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLevelTag, tvTitle, tvContent, tvSensorInfo, tvTime, tvHandle;
        View dotUnread;

        ViewHolder(View itemView) {
            super(itemView);
            tvLevelTag = itemView.findViewById(R.id.tv_level_tag);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvSensorInfo = itemView.findViewById(R.id.tv_sensor_info);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvHandle = itemView.findViewById(R.id.tv_handle);
            dotUnread = itemView.findViewById(R.id.dot_unread);
        }
    }
}
