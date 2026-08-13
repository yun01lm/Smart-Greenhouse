package com.greenhouse.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.greenhouse.app.R;
import com.greenhouse.app.data.model.SceneInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 场景联动列表适配器
 * <p>
 * 每个场景卡片：场景名称 + 操作摘要 + 执行按钮
 * </p>
 */
public class SceneAdapter extends RecyclerView.Adapter<SceneAdapter.SceneViewHolder> {

    private final List<SceneInfo> scenes = new ArrayList<>();
    private OnSceneExecuteListener listener;
    private boolean operating = false;

    public interface OnSceneExecuteListener {
        void onExecute(SceneInfo scene);
    }

    public void setOnSceneExecuteListener(OnSceneExecuteListener listener) {
        this.listener = listener;
    }

    /** 操作进行中：禁用执行按钮，防止重复点击 */
    public void setOperating(boolean operating) {
        this.operating = operating;
        notifyDataSetChanged();
    }

    public void setData(List<SceneInfo> newScenes) {
        scenes.clear();
        if (newScenes != null) scenes.addAll(newScenes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SceneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scene, parent, false);
        return new SceneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SceneViewHolder holder, int position) {
        holder.bind(scenes.get(position));
    }

    @Override
    public int getItemCount() {
        return scenes.size();
    }

    class SceneViewHolder extends RecyclerView.ViewHolder {
        TextView tvSceneName;
        TextView tvSceneDesc;
        Button btnExecute;

        SceneViewHolder(View itemView) {
            super(itemView);
            tvSceneName = itemView.findViewById(R.id.tv_scene_name);
            tvSceneDesc = itemView.findViewById(R.id.tv_scene_desc);
            btnExecute = itemView.findViewById(R.id.btn_execute);
        }

        void bind(SceneInfo scene) {
            tvSceneName.setText(scene.getName());
            tvSceneDesc.setText(scene.getActionsSummary());
            btnExecute.setEnabled(!operating);
            btnExecute.setText(operating ? "执行中..." : "一键执行");
            btnExecute.setOnClickListener(v -> {
                if (listener != null && !operating) {
                    listener.onExecute(scene);
                }
            });
        }
    }
}
