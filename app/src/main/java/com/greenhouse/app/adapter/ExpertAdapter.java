package com.greenhouse.app.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.greenhouse.app.R;
import com.greenhouse.app.data.model.ExpertInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 专家列表适配器 (F10)
 * <p>
 * 展示在线/离线专家，显示姓名、专业领域、评分、咨询次数。
 * 点击"求助"按钮触发咨询对话创建。
 * </p>
 */
public class ExpertAdapter extends RecyclerView.Adapter<ExpertAdapter.ViewHolder> {

    private final List<ExpertInfo> items = new ArrayList<>();
    private OnExpertActionListener listener;

    public interface OnExpertActionListener {
        void onHelpClick(ExpertInfo expert);
    }

    public void setOnExpertActionListener(OnExpertActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<ExpertInfo> newItems) {
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
                .inflate(R.layout.item_expert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpertInfo expert = items.get(position);

        holder.tvName.setText(expert.getRealName());
        holder.tvSpecialty.setText(expert.getExpertSpecialty());
        holder.tvRating.setText("评分 " + expert.getRatingText());
        holder.tvConsultCount.setText(expert.getConsultCountText());

        // 头像：显示姓名首字（替代固定"专"字占位）
        String name = expert.getRealName();
        if (name != null && !name.isEmpty()) {
            holder.tvAvatar.setText(String.valueOf(name.charAt(0)));
        } else {
            holder.tvAvatar.setText("专");
        }

        // 在线状态指示器
        holder.indicatorOnline.setVisibility(expert.isOnline() ? View.VISIBLE : View.GONE);

        // 求助按钮（离线专家禁用）
        holder.btnHelp.setEnabled(expert.isOnline());
        holder.btnHelp.setText(expert.isOnline() ? "求助" : "离线");
        if (!expert.isOnline()) {
            holder.btnHelp.setAlpha(0.5f);
        } else {
            holder.btnHelp.setAlpha(1.0f);
        }

        holder.btnHelp.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHelpClick(expert);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View indicatorOnline;
        TextView tvName, tvSpecialty, tvRating, tvConsultCount, tvAvatar;
        MaterialButton btnHelp;

        ViewHolder(View itemView) {
            super(itemView);
            indicatorOnline = itemView.findViewById(R.id.indicator_online);
            tvName = itemView.findViewById(R.id.tv_name);
            tvSpecialty = itemView.findViewById(R.id.tv_specialty);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvConsultCount = itemView.findViewById(R.id.tv_consult_count);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            btnHelp = itemView.findViewById(R.id.btn_help);
        }
    }
}
