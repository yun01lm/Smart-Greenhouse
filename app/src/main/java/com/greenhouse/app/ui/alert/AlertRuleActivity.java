package com.greenhouse.app.ui.alert;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.greenhouse.app.R;
import com.greenhouse.app.data.model.AlertRuleItem;
import com.greenhouse.app.data.model.AlertRuleRequest;
import com.greenhouse.app.data.model.Greenhouse;
import com.greenhouse.app.data.model.SceneInfo;
import com.greenhouse.app.data.repository.AlertRepository;
import com.greenhouse.app.data.repository.ControlRepository;
import com.greenhouse.app.data.repository.SensorRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 预警规则管理（APP 端，第 1 项）
 * <p>
 * 列表 + 新建/编辑（传感器/阈值/级别/联动场景）+ 删除，复用后端 /api/v1/alerts/rules。
 * </p>
 */
public class AlertRuleActivity extends AppCompatActivity {

    private RecyclerView rvRules;
    private RuleAdapter adapter;
    private final List<AlertRuleItem> rules = new ArrayList<>();
    private List<Greenhouse> greenhouses = new ArrayList<>();

    private static final java.util.Map<String, String> SENSOR_CN = new java.util.HashMap<>();
    private static final java.util.Map<String, String> SCENE_CN = new java.util.HashMap<>();
    static {
        SENSOR_CN.put("TEMPERATURE", "空气温度");
        SENSOR_CN.put("HUMIDITY", "空气湿度");
        SENSOR_CN.put("LIGHT", "光照强度");
        SENSOR_CN.put("CO2", "CO₂浓度");
        SENSOR_CN.put("SOIL_TEMP", "土壤温度");
        SENSOR_CN.put("SOIL_MOISTURE", "土壤湿度");
        SENSOR_CN.put("SOIL_PH", "土壤pH");
        SENSOR_CN.put("WIND_SPEED", "风速");
        SCENE_CN.put("fnvcc", "水泵+风机联动");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_rule);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        rvRules = findViewById(R.id.rv_rules);
        rvRules.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RuleAdapter();
        rvRules.setAdapter(adapter);

        findViewById(R.id.btn_new_rule).setOnClickListener(v -> showRuleDialog(null));

        loadGreenhouses();
    }

    private void loadGreenhouses() {
        new SensorRepository().getGreenhouses(new SensorRepository.Callback<List<Greenhouse>>() {
            @Override
            public void onSuccess(List<Greenhouse> data) {
                greenhouses = data != null ? data : new ArrayList<>();
                loadRules();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AlertRuleActivity.this, "加载大棚失败: " + message, Toast.LENGTH_SHORT).show();
                loadRules();
            }
        });
    }

    private void loadRules() {
        if (greenhouses.isEmpty()) return;
        new AlertRepository().getAlertRules(greenhouses.get(0).getId(),
                new SensorRepository.Callback<List<AlertRuleItem>>() {
                    @Override
                    public void onSuccess(List<AlertRuleItem> data) {
                        rules.clear();
                        if (data != null) rules.addAll(data);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(AlertRuleActivity.this, "加载规则失败: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRuleDialog(AlertRuleItem editing) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_alert_rule, null);
        android.widget.Spinner spGh = v.findViewById(R.id.sp_greenhouse);
        android.widget.Spinner spSensor = v.findViewById(R.id.sp_sensor);
        android.widget.Spinner spLevel = v.findViewById(R.id.sp_level);
        android.widget.Spinner spScene = v.findViewById(R.id.sp_scene);
        EditText etMin = v.findViewById(R.id.et_min);
        EditText etMax = v.findViewById(R.id.et_max);
        MaterialButton btnSwitch = v.findViewById(R.id.btn_enabled);

        // 大棚
        String[] ghNames = new String[greenhouses.size()];
        for (int i = 0; i < greenhouses.size(); i++) ghNames[i] = greenhouses.get(i).getName();
        spGh.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ghNames));

        // 传感器（后端枚举）
        String[] sensorKeys = {"TEMPERATURE", "HUMIDITY", "LIGHT", "CO2", "SOIL_TEMP", "SOIL_MOISTURE", "SOIL_PH", "WIND_SPEED"};
        String[] sensorLabels = new String[sensorKeys.length];
        for (int i = 0; i < sensorKeys.length; i++) sensorLabels[i] = SENSOR_CN.getOrDefault(sensorKeys[i], sensorKeys[i]);
        spSensor.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, sensorLabels));

        // 级别
        spLevel.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"提示", "警告", "严重"}));

        // 场景（预载第一个大棚的场景，编辑时按需切换）
        long ghId = greenhouses.isEmpty() ? 1 : greenhouses.get(0).getId();
        loadScenes(ghId, spScene);

        final boolean[] enabledRef = {true};
        if (editing != null) {
            int gi = 0;
            for (int i = 0; i < greenhouses.size(); i++) {
                if (greenhouses.get(i).getId() == editing.getGreenhouseId()) gi = i;
            }
            spGh.setSelection(gi);
            for (int i = 0; i < sensorKeys.length; i++) {
                if (sensorKeys[i].equals(editing.getSensorType())) spSensor.setSelection(i);
            }
            String lv = editing.getAlertLevel() == null ? "WARNING" : editing.getAlertLevel();
            spLevel.setSelection("CRITICAL".equals(lv) ? 2 : "INFO".equals(lv) ? 0 : 1);
            etMin.setText(editing.getMin() != null ? String.valueOf(editing.getMin()) : "");
            etMax.setText(editing.getMax() != null ? String.valueOf(editing.getMax()) : "");
            enabledRef[0] = editing.isEnabled();
        }
        btnSwitch.setText(enabledRef[0] ? "启用中" : "已停用");

        // 切换大棚时刷新场景
        spGh.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v2, int pos, long id) {
                if (!greenhouses.isEmpty()) loadScenes(greenhouses.get(pos).getId(), spScene);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });

        new AlertDialog.Builder(this)
                .setTitle(editing == null ? "新建预警规则" : "编辑预警规则")
                .setView(v)
                .setPositiveButton("保存", (d, w) -> saveRule(editing, spGh, spSensor, spLevel, spScene, etMin, etMax, enabledRef[0]))
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadScenes(long ghId, android.widget.Spinner spScene) {
        new ControlRepository().getScenes(ghId, new ControlRepository.Callback<List<SceneInfo>>() {
            @Override
            public void onSuccess(List<SceneInfo> data) {
                List<String> labels = new ArrayList<>();
                labels.add("不联动");
                if (data != null) {
                    for (SceneInfo s : data) {
                        String raw = s.getName();
                        labels.add(SCENE_CN.getOrDefault(raw, raw));
                    }
                }
                spScene.setAdapter(new android.widget.ArrayAdapter<>(AlertRuleActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, labels));
            }

            @Override
            public void onError(String message) { }
        });
    }

    private void saveRule(AlertRuleItem editing, android.widget.Spinner spGh, android.widget.Spinner spSensor,
                          android.widget.Spinner spLevel, android.widget.Spinner spScene,
                          EditText etMin, EditText etMax, boolean enabled) {
        try {
            String sensorKey = (String) spSensor.getSelectedItem();
            // sensorKey 是中文标签，需要映射回枚举：用索引回查
            int si = spSensor.getSelectedItemPosition();
            String[] keys = {"TEMPERATURE", "HUMIDITY", "LIGHT", "CO2", "SOIL_TEMP", "SOIL_MOISTURE", "SOIL_PH", "WIND_SPEED"};
            sensorKey = keys[si];

            String minStr = etMin.getText().toString().trim();
            String maxStr = etMax.getText().toString().trim();
            if (minStr.isEmpty() && maxStr.isEmpty()) {
                Toast.makeText(this, "请填写最小/最大值", Toast.LENGTH_SHORT).show();
                return;
            }
            String json = "{\"min\":" + (minStr.isEmpty() ? "null" : minStr)
                    + ",\"max\":" + (maxStr.isEmpty() ? "null" : maxStr) + "}";
            String level = new String[]{"INFO", "WARNING", "CRITICAL"}[spLevel.getSelectedItemPosition()];
            Long sceneId = spScene.getSelectedItemPosition() <= 0 ? null
                    : (long) (spScene.getSelectedItemPosition()); // 场景 id 以 1 为下标（不联动为 0）
            long ghId = greenhouses.isEmpty() ? 1 : greenhouses.get(spGh.getSelectedItemPosition()).getId();

            AlertRuleRequest req = new AlertRuleRequest(ghId, sensorKey, "THRESHOLD", json, level, sceneId, enabled);
            if (editing == null) {
                new AlertRepository().createAlertRule(req, new SensorRepository.Callback<AlertRuleItem>() {
                    @Override public void onSuccess(AlertRuleItem data) {
                        Toast.makeText(AlertRuleActivity.this, "规则已创建", Toast.LENGTH_SHORT).show();
                        loadRules();
                    }
                    @Override public void onError(String message) {
                        Toast.makeText(AlertRuleActivity.this, "创建失败: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                new AlertRepository().updateAlertRule(editing.getId(), req, new SensorRepository.Callback<AlertRuleItem>() {
                    @Override public void onSuccess(AlertRuleItem data) {
                        Toast.makeText(AlertRuleActivity.this, "规则已更新", Toast.LENGTH_SHORT).show();
                        loadRules();
                    }
                    @Override public void onError(String message) {
                        Toast.makeText(AlertRuleActivity.this, "更新失败: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ===== 规则列表适配器 =====

    private class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(AlertRuleActivity.this)
                    .inflate(R.layout.item_alert_rule, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            AlertRuleItem r = rules.get(pos);
            h.tvSensor.setText(SENSOR_CN.getOrDefault(r.getSensorType(), r.getSensorType()));
            h.tvCondition.setText((r.getMin() != null ? "最低 " + r.getMin() : "")
                    + (r.getMin() != null && r.getMax() != null ? "  ~  " : "")
                    + (r.getMax() != null ? "最高 " + r.getMax() : ""));
            h.tvLevel.setText(new String[]{"提示", "警告", "严重"}[
                    "CRITICAL".equals(r.getAlertLevel()) ? 2 : "INFO".equals(r.getAlertLevel()) ? 0 : 1]);
            h.tvLevel.setTextColor(0xFFFF6B6B == 0xFFB71C1C ? 0xFFFF6B6B :
                    "CRITICAL".equals(r.getAlertLevel()) ? 0xFFFF6B6B
                            : "INFO".equals(r.getAlertLevel()) ? 0xFF64B5F6 : 0xFFFFB74D);
            h.tvScene.setText(r.getSceneId() != null
                    ? "联动: " + SCENE_CN.getOrDefault(r.getSceneId().toString(), "场景#" + r.getSceneId())
                    : "未联动");
            h.tvEnabled.setText(r.isEnabled() ? "启用" : "停用");
            h.itemView.setOnClickListener(v -> showRuleDialog(r));
            h.btnDelete.setOnClickListener(v -> deleteRule(r));
        }

        @Override
        public int getItemCount() { return rules.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvSensor, tvCondition, tvLevel, tvScene, tvEnabled;
            View btnDelete;
            Holder(@NonNull View itemView) {
                super(itemView);
                tvSensor = itemView.findViewById(R.id.tv_sensor);
                tvCondition = itemView.findViewById(R.id.tv_condition);
                tvLevel = itemView.findViewById(R.id.tv_level);
                tvScene = itemView.findViewById(R.id.tv_scene);
                tvEnabled = itemView.findViewById(R.id.tv_enabled);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }

    private void deleteRule(AlertRuleItem r) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除预警规则")
                .setMessage("确定删除该规则？")
                .setPositiveButton("删除", (d, w) ->
                        new AlertRepository().deleteAlertRule(r.getId(), new SensorRepository.Callback<Void>() {
                            @Override public void onSuccess(Void data) {
                                Toast.makeText(AlertRuleActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                                loadRules();
                            }
                            @Override public void onError(String message) {
                                Toast.makeText(AlertRuleActivity.this, "删除失败: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton("取消", null)
                .show();
    }
}
