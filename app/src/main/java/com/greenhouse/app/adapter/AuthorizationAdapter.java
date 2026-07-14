package com.greenhouse.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.greenhouse.app.R;
import com.greenhouse.app.data.model.AuthorizationInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 授权列表适配器 (F10)
 * <p>
 * 支持两种模式：
 * - 待处理模式：显示"同意"/"拒绝"按钮
 * - 已授权模式：显示"撤销"按钮
 * </p>
 */
public class AuthorizationAdapter extends RecyclerView.Adapter<AuthorizationAdapter.ViewHolder> {

    private final List<AuthorizationInfo> items = new ArrayList<>();
    private final boolean isPendingMode;
    private OnAuthorizationActionListener listener;

    public interface OnAuthorizationActionListener {
        void onApprove(AuthorizationInfo auth);
        void onReject(AuthorizationInfo auth);
        void onRevoke(AuthorizationInfo auth);
    }

    public AuthorizationAdapter(boolean isPendingMode) {
        this.isPendingMode = isPendingMode;
    }

    public void setOnAuthorizationActionListener(OnAuthorizationActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<AuthorizationInfo> newItems) {
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
                .inflate(R.layout.item_authorization, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AuthorizationInfo auth = items.get(position);

        holder.tvExpertName.setText(auth.getExpertName());
        holder.tvGreenhouseName.setText(auth.getGreenhouseName());
        holder.tvReason.setText(auth.getReason() != null ? auth.getReason() : "");
        holder.tvRequestTime.setText("请求时间: " + (auth.getRequestedAt() != null ? auth.getRequestedAt() : "--"));
        holder.tvStatus.setText(auth.getStatusText());

        // 根据模式显示/隐藏按钮
        if (isPendingMode) {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnRevoke.setVisibility(View.GONE);
            holder.tvExpiresTime.setVisibility(View.GONE);

            holder.btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(auth);
            });
            holder.btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(auth);
            });
        } else {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnRevoke.setVisibility(View.VISIBLE);
            holder.tvExpiresTime.setVisibility(View.VISIBLE);
            holder.tvExpiresTime.setText("到期: " + (auth.getExpiresIn() != null ? auth.getExpiresIn() : "--"));

            holder.btnRevoke.setOnClickListener(v -> {
                if (listener != null) listener.onRevoke(auth);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpertName, tvStatus, tvGreenhouseName, tvReason, tvRequestTime, tvExpiresTime;
        LinearLayout layoutActions;
        MaterialButton btnApprove, btnReject, btnRevoke;

        ViewHolder(View itemView) {
            super(itemView);
            tvExpertName = itemView.findViewById(R.id.tv_expert_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvGreenhouseName = itemView.findViewById(R.id.tv_greenhouse_name);
            tvReason = itemView.findViewById(R.id.tv_reason);
            tvRequestTime = itemView.findViewById(R.id.tv_request_time);
            tvExpiresTime = itemView.findViewById(R.id.tv_expires_time);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnRevoke = itemView.findViewById(R.id.btn_revoke);
        }
    }
}
