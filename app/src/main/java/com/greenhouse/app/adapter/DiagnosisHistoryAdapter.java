package com.greenhouse.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.greenhouse.app.R;
import com.greenhouse.app.data.model.DiagnosisHistoryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 诊断历史 RecyclerView 适配器
 */
public class DiagnosisHistoryAdapter extends RecyclerView.Adapter<DiagnosisHistoryAdapter.ViewHolder> {

    private final List<DiagnosisHistoryItem> items = new ArrayList<>();
    private OnItemClickListener listener;
    private String baseUrl;

    public interface OnItemClickListener {
        void onItemClick(DiagnosisHistoryItem item);
    }

    public DiagnosisHistoryAdapter(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<DiagnosisHistoryItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addItems(List<DiagnosisHistoryItem> newItems) {
        if (newItems != null && !newItems.isEmpty()) {
            int startPos = items.size();
            items.addAll(newItems);
            notifyItemRangeInserted(startPos, newItems.size());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diagnosis_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DiagnosisHistoryItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvDiseaseName;
        TextView tvConfidence;
        TextView tvTime;
        View vConfidenceDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvDiseaseName = itemView.findViewById(R.id.tv_disease_name);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
            tvTime = itemView.findViewById(R.id.tv_time);
            vConfidenceDot = itemView.findViewById(R.id.v_confidence_dot);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(items.get(pos));
                }
            });
        }

        void bind(DiagnosisHistoryItem item) {
            // 缩略图
            if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
                String url = item.getImagePath().startsWith("http")
                        ? item.getImagePath() : baseUrl + item.getImagePath();
                Glide.with(itemView.getContext())
                        .load(url)
                        .placeholder(R.drawable.ic_camera)
                        .centerCrop()
                        .into(ivThumbnail);
            }

            // 病害名称
            tvDiseaseName.setText(item.getDiseaseName() != null
                    ? item.getDiseaseName() : "未知病害");

            // 置信度
            tvConfidence.setText(item.getConfidenceText());

            // 置信度颜色点：绿(>=80%) / 黄(70-80%) / 红(<70%)
            int level = item.getConfidenceLevel();
            int colorRes;
            if (level == 0) {
                colorRes = R.color.confidence_high;
            } else if (level == 1) {
                colorRes = R.color.confidence_medium;
            } else {
                colorRes = R.color.confidence_low;
            }
            vConfidenceDot.setBackgroundResource(colorRes);

            // 时间（ISO → 易读格式，如 2026-08-10T01:11:43.199186 → 2026-08-10 01:11）
            tvTime.setText(formatTime(item.getCreatedAt()));
        }
    }

    /** ISO-8601 时间 → 易读格式（2026-08-10T01:11:43.199186 → 2026-08-10 01:11） */
    private String formatTime(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String t = raw;
        int dotIdx = t.indexOf('.');
        if (dotIdx > 0) t = t.substring(0, dotIdx);
        if (t.length() >= 16) {
            return t.substring(0, 10) + " " + t.substring(11, 16);
        }
        return t;
    }
}
