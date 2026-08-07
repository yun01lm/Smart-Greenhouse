package com.greenhouse.app.ui.employee;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.greenhouse.app.R;
import com.greenhouse.app.adapter.EmployeeAdapter;
import com.greenhouse.app.data.model.AddEmployeeRequest;
import com.greenhouse.app.data.model.ApiResponse;
import com.greenhouse.app.data.model.EmployeeItem;
import com.greenhouse.app.data.model.EmployeePermissionItem;
import com.greenhouse.app.data.model.Greenhouse;
import com.greenhouse.app.data.repository.BaseRepository;
import com.greenhouse.app.databinding.ActivityEmployeeManagementBinding;
import com.greenhouse.app.viewmodel.EmployeeViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

/**
 * 员工管理页（棚主端，R26）
 * <p>
 * 员工列表 + 新增（创建/邀请双模式）+ 权限设置 + 重置密码 + 移除员工。
 * 业务逻辑在 EmployeeViewModel，网络在 EmployeeRepository。
 * </p>
 */
public class EmployeeManagementActivity extends AppCompatActivity {

    private ActivityEmployeeManagementBinding binding;
    private EmployeeViewModel viewModel;
    private EmployeeAdapter adapter;

    /** 大棚列表（新增员工时选择授权大棚） */
    private final List<Greenhouse> greenhouses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmployeeManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(EmployeeViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // 列表
        adapter = new EmployeeAdapter();
        binding.rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEmployees.setAdapter(adapter);

        adapter.setOnActionListener(new EmployeeAdapter.OnActionListener() {
            @Override
            public void onEditPermission(EmployeeItem item) { showPermissionDialog(item); }

            @Override
            public void onResetPassword(EmployeeItem item) { showResetPasswordDialog(item); }

            @Override
            public void onRemove(EmployeeItem item) { showRemoveConfirm(item); }
        });

        binding.btnAddEmployee.setOnClickListener(v -> showAddEmployeeDialog());

        observeViewModel();
        loadGreenhouses();
        viewModel.loadEmployees();
    }

    private void observeViewModel() {
        viewModel.getEmployees().observe(this, employees -> {
            adapter.setData(employees);
            binding.tvEmpty.setVisibility(employees == null || employees.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (loading != null && loading) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.consumeMessage();
            }
        });
    }

    // ===== 大棚加载 =====

    private void loadGreenhouses() {
        Call<ApiResponse<List<Greenhouse>>> call = new BaseRepository() {}.apiService.getGreenhouses();
        call.enqueue(new retrofit2.Callback<ApiResponse<List<Greenhouse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Greenhouse>>> c,
                                   retrofit2.Response<ApiResponse<List<Greenhouse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    greenhouses.clear();
                    greenhouses.addAll(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Greenhouse>>> c, Throwable t) {
                // 大棚加载失败不阻塞员工管理
            }
        });
    }

    // ===== 新增员工对话框 =====

    private void showAddEmployeeDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        // 模式选择：创建账号 / 邀请已有账号
        RadioGroup modeGroup = new RadioGroup(this);
        RadioButton rbCreate = new RadioButton(this);
        rbCreate.setText("创建账号");
        rbCreate.setChecked(true);
        RadioButton rbInvite = new RadioButton(this);
        rbInvite.setText("邀请已有账号");
        modeGroup.addView(rbCreate);
        modeGroup.addView(rbInvite);
        root.addView(modeGroup);

        // 邀请模式输入（用户名/手机号）
        EditText etIdentifier = new EditText(this);
        etIdentifier.setHint("已存在员工账号的用户名或手机号（邀请模式）");
        etIdentifier.setVisibility(View.GONE);
        root.addView(etIdentifier);

        // 创建模式输入
        EditText etUsername = label(this, root, "用户名", false);
        EditText etRealName = label(this, root, "真实姓名", false);
        EditText etPhone = label(this, root, "手机号", false);
        EditText etPassword = label(this, root, "初始密码", true);
        etPassword.setHint("至少8位，含字母和数字");

        // 员工类型
        TextView tvType = labelText(this, root, "员工类型");
        RadioGroup typeGroup = new RadioGroup(this);
        RadioButton rbWorker = new RadioButton(this);
        rbWorker.setText("普通员工");
        rbWorker.setChecked(true);
        RadioButton rbTechnician = new RadioButton(this);
        rbTechnician.setText("技术员（默认权限全开）");
        typeGroup.addView(rbWorker);
        typeGroup.addView(rbTechnician);

        // 授权大棚
        TextView tvGh = labelText(this, root, "授权大棚");
        Spinner spinnerGh = new Spinner(this);
        List<String> ghNames = new ArrayList<>();
        for (Greenhouse gh : greenhouses) {
            ghNames.add(gh.getName());
        }
        spinnerGh.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, ghNames));

        root.addView(tvType);
        root.addView(typeGroup);
        root.addView(tvGh);
        root.addView(spinnerGh);

        // 模式切换：显示/隐藏对应字段
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean invite = checkedId == rbInvite.getId();
            etIdentifier.setVisibility(invite ? View.VISIBLE : View.GONE);
            int v = invite ? View.GONE : View.VISIBLE;
            etUsername.setVisibility(v);
            etRealName.setVisibility(v);
            etPhone.setVisibility(v);
            etPassword.setVisibility(v);
            tvType.setVisibility(v);
            typeGroup.setVisibility(v);
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("新增员工")
                .setView(root)
                .setPositiveButton("确认添加", (dialog, which) -> {
                    if (greenhouses.isEmpty()) {
                        Toast.makeText(this, "暂无可选大棚，请先创建大棚", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AddEmployeeRequest request = new AddEmployeeRequest();
                    request.setGreenhouseId(greenhouses.get(spinnerGh.getSelectedItemPosition()).getId());

                    boolean invite = rbInvite.isChecked();
                    if (invite) {
                        String identifier = etIdentifier.getText().toString().trim();
                        if (identifier.isEmpty()) {
                            Toast.makeText(this, "请输入用户名或手机号", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        request.setIdentifier(identifier);
                    } else {
                        String username = etUsername.getText().toString().trim();
                        String realName = etRealName.getText().toString().trim();
                        String phone = etPhone.getText().toString().trim();
                        String password = etPassword.getText().toString();

                        if (username.isEmpty() || password.isEmpty()) {
                            Toast.makeText(this, "用户名和初始密码必填", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        request.setUsername(username);
                        request.setRealName(realName);
                        request.setPhone(phone);
                        request.setPassword(password);
                        request.setRoleType(rbTechnician.isChecked() ? "TECHNICIAN" : "WORKER");
                    }
                    viewModel.addEmployee(request);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ===== 权限设置对话框 =====

    private void showPermissionDialog(EmployeeItem item) {
        String name = item.getRealName() != null && !item.getRealName().isEmpty()
                ? item.getRealName() : item.getUsername();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("权限设置 - " + name)
                .setNegativeButton("取消", null);

        viewModel.loadPermissions(item.getId(), permissions -> {
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(16);
            root.setPadding(pad, pad, pad, pad);

            if (permissions == null || permissions.isEmpty()) {
                TextView tv = new TextView(this);
                tv.setText("该员工暂无权限记录");
                tv.setTextColor(getColor(R.color.text_secondary));
                root.addView(tv);
            } else {
                for (EmployeePermissionItem p : permissions) {
                    TextView tvTitle = new TextView(this);
                    tvTitle.setText("大棚：" + p.getGreenhouseName());
                    tvTitle.setTextColor(getColor(R.color.on_surface));
                    tvTitle.setTextSize(15);
                    tvTitle.setPadding(0, dp(4), 0, dp(4));
                    root.addView(tvTitle);

                    CheckBox cbViewData = new CheckBox(this);
                    cbViewData.setText("查看数据");
                    cbViewData.setChecked(p.isCanViewData());
                    cbViewData.setOnCheckedChangeListener((b, v) -> p.setCanViewData(v));
                    root.addView(cbViewData);

                    CheckBox cbControl = new CheckBox(this);
                    cbControl.setText("控制设备");
                    cbControl.setChecked(p.isCanControlDevice());
                    cbControl.setOnCheckedChangeListener((b, v) -> p.setCanControlDevice(v));
                    root.addView(cbControl);

                    CheckBox cbDiagnose = new CheckBox(this);
                    cbDiagnose.setText("病虫害诊断");
                    cbDiagnose.setChecked(p.isCanDiagnose());
                    cbDiagnose.setOnCheckedChangeListener((b, v) -> p.setCanDiagnose(v));
                    root.addView(cbDiagnose);

                    CheckBox cbAskExpert = new CheckBox(this);
                    cbAskExpert.setText("专家咨询");
                    cbAskExpert.setChecked(p.isCanAskExpert());
                    cbAskExpert.setOnCheckedChangeListener((b, v) -> p.setCanAskExpert(v));
                    root.addView(cbAskExpert);

                    CheckBox cbAlerts = new CheckBox(this);
                    cbAlerts.setText("查看预警");
                    cbAlerts.setChecked(p.isCanViewAlerts());
                    cbAlerts.setOnCheckedChangeListener((b, v) -> p.setCanViewAlerts(v));
                    root.addView(cbAlerts);

                    CheckBox cbHistory = new CheckBox(this);
                    cbHistory.setText("查看历史");
                    cbHistory.setChecked(p.isCanViewHistory());
                    cbHistory.setOnCheckedChangeListener((b, v) -> p.setCanViewHistory(v));
                    root.addView(cbHistory);

                    View divider = new View(this);
                    divider.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    divider.setBackgroundColor(getColor(R.color.primary_light));
                    root.addView(divider);
                }
            }

            builder.setView(root);
            builder.setPositiveButton("保存", (dialog, which) -> {
                if (permissions != null && !permissions.isEmpty()) {
                    viewModel.savePermissions(item.getId(), permissions);
                }
            });
            builder.show();
        });
    }

    // ===== 重置密码对话框 =====

    private void showResetPasswordDialog(EmployeeItem item) {
        String name = item.getRealName() != null && !item.getRealName().isEmpty()
                ? item.getRealName() : item.getUsername();
        EditText input = new EditText(this);
        input.setHint("至少8位，含字母和数字");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setPadding(dp(16), dp(8), dp(16), dp(8));

        new MaterialAlertDialogBuilder(this)
                .setTitle("重置密码 - " + name)
                .setView(input)
                .setPositiveButton("确认重置", (dialog, which) -> {
                    String pwd = input.getText() != null ? input.getText().toString() : "";
                    if (pwd.length() < 8) {
                        Toast.makeText(this, "密码至少8位", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.resetPassword(item.getId(), pwd);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ===== 移除员工 =====

    private void showRemoveConfirm(EmployeeItem item) {
        String name = item.getRealName() != null && !item.getRealName().isEmpty()
                ? item.getRealName() : item.getUsername();
        new MaterialAlertDialogBuilder(this)
                .setTitle("移除员工")
                .setMessage("确定移除「" + name + "」吗？移除后该员工将失去本棚所有大棚的访问权限。")
                .setPositiveButton("确认移除", (dialog, which) -> viewModel.removeEmployee(item.getId()))
                .setNegativeButton("取消", null)
                .show();
    }

    // ===== 工具方法 =====

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private TextView labelText(Context context, LinearLayout parent, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(context.getColor(R.color.on_surface));
        tv.setTextSize(14);
        tv.setPadding(0, dp(12), 0, dp(4));
        return tv;
    }

    private EditText label(Context context, LinearLayout parent, String text, boolean isPassword) {
        parent.addView(labelText(context, parent, text));
        EditText et = new EditText(context);
        et.setHint("请输入" + text);
        if (isPassword) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        parent.addView(et);
        return et;
    }
}