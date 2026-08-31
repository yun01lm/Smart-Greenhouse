package com.greenhouse.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.Greenhouse;
import com.greenhouse.app.databinding.ItemGreenhouseBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 大棚列表适配器（R45）
 */
public class GreenhouseAdapter extends RecyclerView.Adapter<GreenhouseAdapter.VH> {

    public interface OnActionListener {
        void onEdit(Greenhouse item);
        void onDelete(Greenhouse item);
    }

    private final List<Greenhouse> data = new ArrayList<>();
    private OnActionListener listener;

    public void setData(List<Greenhouse> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public void setOnActionListener(OnActionListener l) { this.listener = l; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGreenhouseBinding b = ItemGreenhouseBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Greenhouse gh = data.get(position);
        holder.binding.tvName.setText(gh.getName());
        if (gh.getCropType() != null && !gh.getCropType().isEmpty()) {
            holder.binding.tvCrop.setText(gh.getCropType());
            holder.binding.tvCrop.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvCrop.setVisibility(View.GONE);
        }
        String region = join(gh.getProvince(), gh.getCity(), gh.getDistrict());
        holder.binding.tvRegion.setText(region.isEmpty() ? "地区未设置" : region);
        if (gh.getLocation() != null && !gh.getLocation().isEmpty()) {
            holder.binding.tvLocation.setText(gh.getLocation());
            holder.binding.tvLocation.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvLocation.setVisibility(View.GONE);
        }
        holder.binding.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(gh);
        });
        holder.binding.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(gh);
        });
    }

    private String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(p);
            }
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemGreenhouseBinding binding;
        VH(ItemGreenhouseBinding b) { super(b.getRoot()); binding = b; }
    }
}
