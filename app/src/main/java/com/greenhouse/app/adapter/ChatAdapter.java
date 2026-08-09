package com.greenhouse.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.QaResponse;
import com.greenhouse.app.viewmodel.QaViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天气泡 RecyclerView 适配器
 * <p>
 * 支持三种类型：用户消息（右对齐绿色）、AI 回复（左对齐白色+来源引用+TTS）、错误消息（红色居中）
 * </p>
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_AI = 1;
    private static final int VIEW_TYPE_ERROR = 2;

    private final List<QaViewModel.ChatMessage> messages = new ArrayList<>();
    private OnTtsClickListener ttsListener;

    public interface OnTtsClickListener {
        void onTtsClick(String text);
    }

    public void setOnTtsClickListener(OnTtsClickListener listener) {
        this.ttsListener = listener;
    }

    public void setMessages(List<QaViewModel.ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void addMessage(QaViewModel.ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        QaViewModel.ChatMessage msg = messages.get(position);
        if (msg.getType() == QaViewModel.ChatMessage.TYPE_USER) return VIEW_TYPE_USER;
        if (msg.getType() == QaViewModel.ChatMessage.TYPE_AI) return VIEW_TYPE_AI;
        return VIEW_TYPE_ERROR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            return new UserViewHolder(inflater.inflate(R.layout.item_chat_bubble_user, parent, false));
        } else if (viewType == VIEW_TYPE_AI) {
            return new AiViewHolder(inflater.inflate(R.layout.item_chat_bubble_ai, parent, false));
        } else {
            return new ErrorViewHolder(inflater.inflate(R.layout.item_chat_bubble_error, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        QaViewModel.ChatMessage msg = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(msg);
        } else if (holder instanceof AiViewHolder) {
            ((AiViewHolder) holder).bind(msg);
        } else if (holder instanceof ErrorViewHolder) {
            ((ErrorViewHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ===== ViewHolders =====

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvVoiceHint;
        TextView tvTime;

        UserViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvVoiceHint = itemView.findViewById(R.id.tv_voice_hint);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(QaViewModel.ChatMessage msg) {
            tvMessage.setText(msg.getText());
            tvVoiceHint.setVisibility(msg.isVoice() ? View.VISIBLE : View.GONE);
            if (msg.getTimeText() != null && !msg.getTimeText().isEmpty()) {
                tvTime.setText(msg.getTimeText());
                tvTime.setVisibility(View.VISIBLE);
            } else {
                tvTime.setVisibility(View.GONE);
            }
        }
    }

    class AiViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvSources;
        TextView tvTime;
        ImageButton btnTts;
        LinearLayout llSources;

        AiViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvSources = itemView.findViewById(R.id.tv_sources);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnTts = itemView.findViewById(R.id.btn_tts);
            llSources = itemView.findViewById(R.id.ll_sources);
        }

        void bind(QaViewModel.ChatMessage msg) {
            tvMessage.setText(msg.getText());

            // TTS 播放按钮
            btnTts.setOnClickListener(v -> {
                if (ttsListener != null && msg.getText() != null) {
                    ttsListener.onTtsClick(msg.getText());
                }
            });

            // 引用来源
            QaResponse resp = msg.getQaResponse();
            if (resp != null && resp.getSources() != null && !resp.getSources().isEmpty()) {
                StringBuilder sb = new StringBuilder("📚 参考来源：\n");
                for (QaResponse.SourceInfo source : resp.getSources()) {
                    sb.append("• ").append(source.getTitle())
                            .append(" [").append(source.getCategory()).append("]\n");
                }
                tvSources.setText(sb.toString().trim());
                llSources.setVisibility(View.VISIBLE);
            } else {
                llSources.setVisibility(View.GONE);
            }
            if (msg.getTimeText() != null && !msg.getTimeText().isEmpty()) {
                tvTime.setText(msg.getTimeText());
                tvTime.setVisibility(View.VISIBLE);
            } else {
                tvTime.setVisibility(View.GONE);
            }
        }
    }

    static class ErrorViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        ErrorViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
        }

        void bind(QaViewModel.ChatMessage msg) {
            tvMessage.setText("❌ " + msg.getText());
        }
    }
}
