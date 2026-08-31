package com.greenhouse.app.ui.greenhouse;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.greenhouse.app.R;
import com.greenhouse.app.adapter.GreenhouseAdapter;
import com.greenhouse.app.data.api.ApiClient;
import com.greenhouse.app.data.api.GreenhouseApiService;
import com.greenhouse.app.data.model.ApiResponse;
import com.greenhouse.app.data.model.Greenhouse;
import com.greenhouse.app.data.model.GreenhouseRequest;
import com.greenhouse.app.data.repository.BaseRepository;
import com.greenhouse.app.data.repository.SensorRepository;
import com.greenhouse.app.databinding.ActivityGreenhouseManageBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 大棚管理页（棚主端，R45）
 * <p>
 * 大棚列表 + 新增大棚 + 编辑 + 删除（删除级联清理）。
 * </p>
 */
public class GreenhouseManageActivity extends AppCompatActivity {

    private ActivityGreenhouseManageBinding binding;
    private GreenhouseAdapter adapter;
    private final SensorRepository sensorRepository = new SensorRepository();
    private final GreenhouseApiService api = ApiClient.getApiService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGreenhouseManageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new GreenhouseAdapter();
        binding.rvGreenhouses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGreenhouses.setAdapter(adapter);

        adapter.setOnActionListener(new GreenhouseAdapter.OnActionListener() {
            @Override
            public void onEdit(Greenhouse item) { showEditDialog(item); }

            @Override
            public void onDelete(Greenhouse item) { showDeleteConfirm(item); }
        });

        binding.btnAddGreenhouse.setOnClickListener(v -> showAddDialog());
        loadGreenhouses();
    }

    private void loadGreenhouses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        sensorRepository.getGreenhouses(new BaseRepository.Callback<List<Greenhouse>>() {
            @Override
            public void onSuccess(List<Greenhouse> data) {
                binding.progressBar.setVisibility(View.GONE);
                adapter.setData(data);
                binding.tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(GreenhouseManageActivity.this, "加载失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== 新增/编辑弹窗 =====

    private void showAddDialog() {
        showFormDialog(null);
    }

    private void showEditDialog(Greenhouse gh) {
        showFormDialog(gh);
    }

    private void showFormDialog(Greenhouse existing) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(56, 24, 56, 8);

        EditText etName = makeEdit("大棚名称（必填）", existing != null ? existing.getName() : "");
        EditText etCrop = makeEdit("作物类型（可选）", existing != null ? existing.getCropType() : "");
        EditText etProvince = makeEdit("省（可选）", existing != null ? existing.getProvince() : "");
        EditText etCity = makeEdit("市（可选）", existing != null ? existing.getCity() : "");
        EditText etDistrict = makeEdit("区/县（可选）", existing != null ? existing.getDistrict() : "");
        EditText etTown = makeEdit("乡镇（可选）", existing != null ? existing.getTown() : "");
        EditText etVillage = makeEdit("村（可选）", existing != null ? existing.getVillage() : "");
        EditText etLocation = makeEdit("位置描述（可选）", existing != null ? existing.getLocation() : "");

        layout.addView(etName);
        layout.addView(etCrop);
        layout.addView(etProvince);
        layout.addView(etCity);
        layout.addView(etDistrict);
        layout.addView(etTown);
        layout.addView(etVillage);
        layout.addView(etLocation);

        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "新增大棚" : "编辑大棚")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "请输入大棚名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    GreenhouseRequest req = new GreenhouseRequest(
                            name,
                            trim(etCrop), trim(etLocation),
                            trim(etProvince), trim(etCity), trim(etDistrict),
                            trim(etTown), trim(etVillage));
                    if (existing == null) {
                        create(req);
                    } else {
                        update(existing.getId(), req);
                    }
                })
                .show();
    }

    private EditText makeEdit(String hint, String value) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(value == null ? "" : value);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        return et;
    }

    private String trim(EditText et) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? null : s;
    }

    // ===== 提交 =====

    private void create(GreenhouseRequest req) {
        api.createGreenhouse(req).enqueue(new Callback<ApiResponse<Greenhouse>>() {
            @Override
            public void onResponse(Call<ApiResponse<Greenhouse>> call, Response<ApiResponse<Greenhouse>> response) {
                ApiResponse<Greenhouse> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess()) {
                    Toast.makeText(GreenhouseManageActivity.this, "大棚创建成功", Toast.LENGTH_SHORT).show();
                    loadGreenhouses();
                } else {
                    Toast.makeText(GreenhouseManageActivity.this,
                            "创建失败: " + (body != null ? body.getMessage() : "网络错误"), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Greenhouse>> call, Throwable t) {
                Toast.makeText(GreenhouseManageActivity.this, "创建失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void update(long id, GreenhouseRequest req) {
        api.updateGreenhouse(id, req).enqueue(new Callback<ApiResponse<Greenhouse>>() {
            @Override
            public void onResponse(Call<ApiResponse<Greenhouse>> call, Response<ApiResponse<Greenhouse>> response) {
                ApiResponse<Greenhouse> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccess()) {
                    Toast.makeText(GreenhouseManageActivity.this, "大棚已更新", Toast.LENGTH_SHORT).show();
                    loadGreenhouses();
                } else {
                    Toast.makeText(GreenhouseManageActivity.this,
                            "更新失败: " + (body != null ? body.getMessage() : "网络错误"), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Greenhouse>> call, Throwable t) {
                Toast.makeText(GreenhouseManageActivity.this, "更新失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirm(Greenhouse gh) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("删除大棚")
                .setMessage("确认删除「" + gh.getName() + "」吗？其下所有设备（固件将解绑）、预警规则、场景、授权与历史数据将被一并清理，不可恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认删除", (dialog, which) -> {
                    api.deleteGreenhouse(gh.getId()).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                            ApiResponse<Void> body = response.body();
                            if (response.isSuccessful() && body != null && body.isSuccess()) {
                                Toast.makeText(GreenhouseManageActivity.this, "大棚已删除", Toast.LENGTH_SHORT).show();
                                loadGreenhouses();
                            } else {
                                Toast.makeText(GreenhouseManageActivity.this,
                                        "删除失败: " + (body != null ? body.getMessage() : "网络错误"), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(GreenhouseManageActivity.this, "删除失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }
}
