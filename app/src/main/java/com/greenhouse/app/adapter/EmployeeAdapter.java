package com.greenhouse.app.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.EmployeeItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 员工列表适配器（棚主端，R26）
 * <p>展示员工姓名/用户名/类型/手机号，操作按钮回调由 Activity 处理。</p>
 */
public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {

    private final List<EmployeeItem> items = new ArrayList<>();

    /** 操作回调 */
    public interface OnActionListener {
        void onEditPermission(EmployeeItem item);
        void onResetPassword(EmployeeItem item);
        void onRemove(EmployeeItem item);
    }

    private OnActionListener listener;

    public void setOnActionListener(OnActionListener listener) { this.listener = listener; }

    public void setData(List<EmployeeItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<EmployeeItem> getItems() { return items; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmployeeItem item = items.get(position);

        String name = item.getRealName() != null && !item.getRealName().isEmpty()
                ? item.getRealName() : item.getUsername();
        holder.tvName.setText(name);
        holder.tvUsername.setText("@" + item.getUsername());

        String roleText = item.isTechnician() ? "技术员" : "普通员工";
        holder.tvRole.setText(roleText);
        holder.tvRole.setTextColor(holder.itemView.getContext().getColor(
                item.isTechnician() ? R.color.level_attention : R.color.text_secondary));

        holder.tvPhone.setText(item.getPhone() != null && !item.getPhone().isEmpty()
                ? item.getPhone() : "未绑定手机号");

        List<String> ghNames = item.getGreenhouseNames();
        holder.tvGreenhouses.setText(ghNames == null || ghNames.isEmpty()
                ? "未分配大棚"
                : "大棚：" + TextUtils.join("、", ghNames));

        EmployeeItem current = item;
        holder.btnPermission.setOnClickListener(v -> {
            if (listener != null) listener.onEditPermission(current);
        });
        holder.btnResetPwd.setOnClickListener(v -> {
            if (listener != null) listener.onResetPassword(current);
        });
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) listener.onRemove(current);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvUsername;
        final TextView tvRole;
        final TextView tvPhone;
        final TextView tvGreenhouses;
        final TextView btnPermission;
        final TextView btnResetPwd;
        final TextView btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_emp_name);
            tvUsername = itemView.findViewById(R.id.tv_emp_username);
            tvRole = itemView.findViewById(R.id.tv_emp_role);
            tvPhone = itemView.findViewById(R.id.tv_emp_phone);
            tvGreenhouses = itemView.findViewById(R.id.tv_emp_greenhouses);
            btnPermission = itemView.findViewById(R.id.btn_emp_permission);
            btnResetPwd = itemView.findViewById(R.id.btn_emp_reset_pwd);
            btnRemove = itemView.findViewById(R.id.btn_emp_remove);
        }
    }
}