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
import com.greenhouse.app.data.model.ChatMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 聊天消息适配器 (F10)
 * <p>
 * 支持多 ViewType 的消息列表：
 * - TEXT_LEFT：专家发送的文字消息
 * - TEXT_RIGHT：用户发送的文字消息
 * - IMAGE_LEFT / IMAGE_RIGHT：图片消息
 * - SNAPSHOT：环境快照卡片
 * </p>
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TEXT_LEFT = 0;
    private static final int TYPE_TEXT_RIGHT = 1;
    private static final int TYPE_IMAGE_LEFT = 2;
    private static final int TYPE_IMAGE_RIGHT = 3;
    private static final int TYPE_SNAPSHOT = 4;

    private final List<ChatMessage> items = new ArrayList<>();

    public void setData(List<ChatMessage> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessage message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = items.get(position);

        if (msg.isSnapshot()) {
            return TYPE_SNAPSHOT;
        }

        if (msg.isImage()) {
            return msg.isFromUser() ? TYPE_IMAGE_RIGHT : TYPE_IMAGE_LEFT;
        }

        // 默认为文本消息
        return msg.isFromUser() ? TYPE_TEXT_RIGHT : TYPE_TEXT_LEFT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_TEXT_LEFT:
                return new TextLeftHolder(inflater.inflate(R.layout.item_chat_message_left, parent, false));
            case TYPE_TEXT_RIGHT:
                return new TextRightHolder(inflater.inflate(R.layout.item_chat_message_right, parent, false));
            case TYPE_IMAGE_LEFT:
                // 图片左侧使用与文字左侧相同的布局，但 ImageView 替代文本
                View imageLeftView = inflater.inflate(R.layout.item_chat_message_left, parent, false);
                return new ImageLeftHolder(imageLeftView);
            case TYPE_IMAGE_RIGHT:
                View imageRightView = inflater.inflate(R.layout.item_chat_message_right, parent, false);
                return new ImageRightHolder(imageRightView);
            case TYPE_SNAPSHOT:
                return new SnapshotHolder(inflater.inflate(R.layout.item_snapshot_card, parent, false));
            default:
                return new TextLeftHolder(inflater.inflate(R.layout.item_chat_message_left, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = items.get(position);
        String time = formatTime(msg.getCreatedAt());

        int viewType = getItemViewType(position);
        switch (viewType) {
            case TYPE_TEXT_LEFT:
                bindTextLeft((TextLeftHolder) holder, msg, time);
                break;
            case TYPE_TEXT_RIGHT:
                bindTextRight((TextRightHolder) holder, msg, time);
                break;
            case TYPE_IMAGE_LEFT:
                bindImageLeft((ImageLeftHolder) holder, msg, time);
                break;
            case TYPE_IMAGE_RIGHT:
                bindImageRight((ImageRightHolder) holder, msg, time);
                break;
            case TYPE_SNAPSHOT:
                bindSnapshot((SnapshotHolder) holder, msg);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ===== 绑定方法 =====

    private void bindTextLeft(TextLeftHolder holder, ChatMessage msg, String time) {
        holder.tvSenderName.setText(msg.getSenderName() != null ? msg.getSenderName() : "专家");
        holder.tvContent.setText(msg.getContent());
        holder.tvTime.setText(time);
    }

    private void bindTextRight(TextRightHolder holder, ChatMessage msg, String time) {
        holder.tvContent.setText(msg.getContent());
        holder.tvTime.setText(time);
    }

    private void bindImageLeft(ImageLeftHolder holder, ChatMessage msg, String time) {
        holder.tvSenderName.setText(msg.getSenderName() != null ? msg.getSenderName() : "专家");
        holder.tvContent.setVisibility(View.GONE);
        holder.ivImage.setVisibility(View.VISIBLE);
        holder.tvTime.setText(time);

        if (msg.getFilePath() != null && !msg.getFilePath().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(msg.getFilePath())
                    .placeholder(R.drawable.ic_image_attach)
                    .into(holder.ivImage);
        }
    }

    private void bindImageRight(ImageRightHolder holder, ChatMessage msg, String time) {
        holder.tvContent.setVisibility(View.GONE);
        holder.ivImage.setVisibility(View.VISIBLE);
        holder.tvTime.setText(time);

        if (msg.getFilePath() != null && !msg.getFilePath().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(msg.getFilePath())
                    .placeholder(R.drawable.ic_image_attach)
                    .into(holder.ivImage);
        }
    }

    private void bindSnapshot(SnapshotHolder holder, ChatMessage msg) {
        ChatMessage.SnapshotData data = msg.getSnapshotData();
        if (data != null) {
            holder.tvGreenhouseName.setText(data.getGreenhouseName() != null ? data.getGreenhouseName() : "大棚");
            holder.tvAvgTemp.setText(String.format(Locale.getDefault(), "%.1f°C", data.getAvgTemp()));
            holder.tvAvgHumidity.setText(String.format(Locale.getDefault(), "%.1f%%", data.getAvgHumidity()));
            holder.tvSnapshotTime.setText(data.getCapturedAt() != null ? data.getCapturedAt() : "");
        }
    }

    // ===== 时间格式化 =====

    private String formatTime(String isoTime) {
        if (isoTime == null) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(isoTime.replace("Z", ""));
            if (date == null) return isoTime;
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            return isoTime;
        }
    }

    // ===== ViewHolder =====

    static class TextLeftHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvContent, tvTime;

        TextLeftHolder(View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }

    static class TextRightHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;

        TextRightHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }

    static class ImageLeftHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvContent, tvTime;
        ImageView ivImage;

        ImageLeftHolder(View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvTime = itemView.findViewById(R.id.tv_time);
            // 动态添加 ImageView（由于布局共用）
            ivImage = new ImageView(itemView.getContext());
            ivImage.setVisibility(View.GONE);
            ivImage.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            ((ViewGroup) itemView).addView(ivImage, 1);
        }
    }

    static class ImageRightHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;
        ImageView ivImage;

        ImageRightHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivImage = new ImageView(itemView.getContext());
            ivImage.setVisibility(View.GONE);
            ivImage.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            ((ViewGroup) itemView).addView(ivImage, 0);
        }
    }

    static class SnapshotHolder extends RecyclerView.ViewHolder {
        TextView tvGreenhouseName, tvAvgTemp, tvAvgHumidity, tvSnapshotTime;

        SnapshotHolder(View itemView) {
            super(itemView);
            tvGreenhouseName = itemView.findViewById(R.id.tv_greenhouse_name);
            tvAvgTemp = itemView.findViewById(R.id.tv_avg_temp);
            tvAvgHumidity = itemView.findViewById(R.id.tv_avg_humidity);
            tvSnapshotTime = itemView.findViewById(R.id.tv_snapshot_time);
        }
    }
}
