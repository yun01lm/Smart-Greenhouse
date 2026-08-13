package com.greenhouse.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.ConversationInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 会话列表适配器
 * <p>展示"我的咨询"会话列表：专家名、主题、最后消息、状态、未读角标、时间。</p>
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private final List<ConversationInfo> items = new ArrayList<>();
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(ConversationInfo conversation);
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<ConversationInfo> newItems) {
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
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationInfo conv = items.get(position);

        holder.tvExpertName.setText(conv.getExpertName() != null ? conv.getExpertName() : "专家");
        holder.tvSubject.setText(conv.getSubject() != null ? conv.getSubject() : "");
        String last = conv.getLastMessage();
        holder.tvLastMessage.setText(last != null && !last.isEmpty() ? last : "暂无消息");
        holder.tvStatus.setText(conv.getStatusText());
        holder.tvTime.setText(formatTime(conv.getCreatedAt()));

        if (conv.hasUnread()) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(conv.getUnreadBadge());
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conv);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            String normalized = isoTime.length() >= 19 ? isoTime.substring(0, 19) : isoTime;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(normalized.replace("Z", ""));
            if (date == null) return isoTime;
            return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            return isoTime;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpertName, tvSubject, tvLastMessage, tvStatus, tvTime, tvUnreadBadge;

        ViewHolder(View itemView) {
            super(itemView);
            tvExpertName = itemView.findViewById(R.id.tv_expert_name);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvUnreadBadge = itemView.findViewById(R.id.tv_unread_badge);
        }
    }
}