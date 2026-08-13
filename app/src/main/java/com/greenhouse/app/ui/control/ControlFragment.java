package com.greenhouse.app.ui.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.greenhouse.app.adapter.DeviceAdapter;
import com.greenhouse.app.adapter.SceneAdapter;
import com.greenhouse.app.adapter.SceneDevicePickAdapter;
import com.greenhouse.app.data.model.CreateSceneRequest;
import com.greenhouse.app.data.model.DeviceGroup;
import com.greenhouse.app.data.model.DeviceInfo;
import com.greenhouse.app.data.model.SceneInfo;
import com.greenhouse.app.databinding.DialogCreateSceneBinding;
import com.greenhouse.app.databinding.FragmentControlBinding;
import com.greenhouse.app.util.RoleAdapter;
import com.greenhouse.app.viewmodel.ControlViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备控制页面 (F05)
 * <p>
 * 按大棚分组展示设备列表（支持开关控制）、场景联动（一键执行 + 添加场景）。
 * 符合规范：Fragment 只负责 UI，业务逻辑在 ViewModel。
 * </p>
 */
public class ControlFragment extends Fragment {

    private FragmentControlBinding binding;
    private ControlViewModel viewModel;
    private DeviceAdapter deviceAdapter;
    private SceneAdapter sceneAdapter;
    private SceneDevicePickAdapter scenePickAdapter;

    // 最新分组数据缓存（创建场景对话框选择大棚/设备用）
    private final List<DeviceGroup> cachedGroups = new ArrayList<>();

    // 默认大棚ID，实际从 MainActivity 或 SharedPreferences 获取
    private long currentGreenhouseId = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentControlBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ControlViewModel.class);
        viewModel.setCurrentGreenhouseId(currentGreenhouseId);

        // 设备列表 RecyclerView
        deviceAdapter = new DeviceAdapter();
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDevices.setHasFixedSize(true);
        binding.rvDevices.setAdapter(deviceAdapter);
        deviceAdapter.setOnDeviceSwitchListener((device, turnOn) -> {
            viewModel.controlActuator(device, turnOn ? "ON" : "OFF");
        });

        // 场景列表 RecyclerView
        sceneAdapter = new SceneAdapter();
        binding.rvScenes.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvScenes.setHasFixedSize(true);
        binding.rvScenes.setAdapter(sceneAdapter);
        sceneAdapter.setOnSceneExecuteListener(scene -> {
            viewModel.executeScene(scene);
        });

        // 添加场景入口
        binding.btnAddScene.setOnClickListener(v -> showCreateSceneDialog());

        // 观察数据
        viewModel.getDeviceGroups().observe(getViewLifecycleOwner(), groups -> {
            cachedGroups.clear();
            if (groups != null) cachedGroups.addAll(groups);
            deviceAdapter.setGroups(groups);
            int totalDevices = 0;
            if (groups != null) {
                for (DeviceGroup group : groups) {
                    totalDevices += group.getDeviceCount();
                }
            }
            if (totalDevices == 0) {
                binding.tvDevicesEmpty.setVisibility(View.VISIBLE);
                binding.rvDevices.setVisibility(View.GONE);
            } else {
                binding.tvDevicesEmpty.setVisibility(View.GONE);
                binding.rvDevices.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getScenes().observe(getViewLifecycleOwner(), scenes -> {
            sceneAdapter.setData(scenes);
            if (scenes == null || scenes.isEmpty()) {
                binding.tvScenesEmpty.setVisibility(View.VISIBLE);
                binding.rvScenes.setVisibility(View.GONE);
            } else {
                binding.tvScenesEmpty.setVisibility(View.GONE);
                binding.rvScenes.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getActionResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && !result.isEmpty()) {
                Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // 加载数据
        viewModel.loadDeviceGroups();
        viewModel.loadAllScenes();

        // ===== F11 角色适配：员工无控制权限时禁用设备控制 =====
        if (!RoleAdapter.canControlDevice()) {
            binding.rvDevices.setVisibility(View.GONE);
            binding.rvScenes.setVisibility(View.GONE);
            binding.tvDevicesEmpty.setVisibility(View.VISIBLE);
            binding.tvDevicesEmpty.setText("您没有设备控制权限，请联系棚主授权");
            binding.tvScenesEmpty.setVisibility(View.GONE);
        }
    }

    public void setGreenhouseId(long greenhouseId) {
        this.currentGreenhouseId = greenhouseId;
        if (viewModel != null) {
            viewModel.setCurrentGreenhouseId(greenhouseId);
            viewModel.loadDeviceGroups();
            viewModel.loadAllScenes();
        }
    }

    // ===== 添加场景对话框 =====

    private void showCreateSceneDialog() {
        if (cachedGroups.isEmpty()) {
            Toast.makeText(requireContext(), "暂无大棚数据，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogCreateSceneBinding dialogBinding = DialogCreateSceneBinding.inflate(getLayoutInflater());

        // 大棚选择
        String[] greenhouseNames = new String[cachedGroups.size()];
        for (int i = 0; i < cachedGroups.size(); i++) {
            greenhouseNames[i] = cachedGroups.get(i).getGreenhouseName();
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, greenhouseNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spSceneGreenhouse.setAdapter(spinnerAdapter);

        int defaultPosition = 0;
        for (int i = 0; i < cachedGroups.size(); i++) {
            if (cachedGroups.get(i).getGreenhouseId() == currentGreenhouseId) {
                defaultPosition = i;
                break;
            }
        }
        dialogBinding.spSceneGreenhouse.setSelection(defaultPosition);

        // 设备动作选择
        scenePickAdapter = new SceneDevicePickAdapter();
        dialogBinding.rvScenePick.setLayoutManager(new LinearLayoutManager(requireContext()));
        dialogBinding.rvScenePick.setAdapter(scenePickAdapter);
        updatePickDevices(dialogBinding, defaultPosition);
        dialogBinding.spSceneGreenhouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePickDevices(dialogBinding, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("添加场景")
                .setView(dialogBinding.getRoot())
                .setNegativeButton("取消", null)
                .setPositiveButton("创建", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = dialogBinding.etSceneName.getText() != null
                    ? dialogBinding.etSceneName.getText().toString().trim() : "";
            String description = dialogBinding.etSceneDesc.getText() != null
                    ? dialogBinding.etSceneDesc.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入场景名称", Toast.LENGTH_SHORT).show();
                return;
            }

            List<CreateSceneRequest.SceneActionItem> actions = new ArrayList<>();
            for (int i = 0; i < scenePickAdapter.getItemCount(); i++) {
                if (scenePickAdapter.isChecked(i)) {
                    actions.add(new CreateSceneRequest.SceneActionItem(
                            scenePickAdapter.getDevice(i).getId(),
                            scenePickAdapter.isOn(i) ? "ON" : "OFF"));
                }
            }
            if (actions.isEmpty()) {
                Toast.makeText(requireContext(), "请至少勾选一个设备动作", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            int position = dialogBinding.spSceneGreenhouse.getSelectedItemPosition();
            long greenhouseId = cachedGroups.get(position).getGreenhouseId();
            viewModel.createScene(greenhouseId, name, description, actions);
        }));
        dialog.show();
    }

    private void updatePickDevices(DialogCreateSceneBinding dialogBinding, int position) {
        if (position < 0 || position >= cachedGroups.size()) {
            scenePickAdapter.setDevices(new ArrayList<>());
            dialogBinding.tvPickEmpty.setVisibility(View.VISIBLE);
            return;
        }
        List<DeviceInfo> controllers = new ArrayList<>();
        DeviceGroup group = cachedGroups.get(position);
        if (group.getDevices() != null) {
            for (DeviceInfo device : group.getDevices()) {
                if (device.isController()) {
                    controllers.add(device);
                }
            }
        }
        scenePickAdapter.setDevices(controllers);
        dialogBinding.tvPickEmpty.setVisibility(controllers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
