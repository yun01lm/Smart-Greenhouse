package com.greenhouse.app.ui.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.greenhouse.app.adapter.DeviceAdapter;
import com.greenhouse.app.adapter.SceneAdapter;
import com.greenhouse.app.data.model.ActuatorInfo;
import com.greenhouse.app.data.model.SceneInfo;
import com.greenhouse.app.databinding.FragmentControlBinding;
import com.greenhouse.app.util.RoleAdapter;
import com.greenhouse.app.viewmodel.ControlViewModel;

/**
 * 设备控制页面 (F05)
 * <p>
 * 展示设备列表（支持开关控制）和场景联动按钮。
 * 符合规范：Fragment 只负责 UI，业务逻辑在 ViewModel。
 * </p>
 */
public class ControlFragment extends Fragment {

    private FragmentControlBinding binding;
    private ControlViewModel viewModel;
    private DeviceAdapter deviceAdapter;
    private SceneAdapter sceneAdapter;

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
        binding.rvDevices.setAdapter(deviceAdapter);
        deviceAdapter.setOnDeviceSwitchListener((device, turnOn) -> {
            viewModel.controlActuator(device, turnOn ? "ON" : "OFF");
        });

        // 场景列表 RecyclerView
        sceneAdapter = new SceneAdapter();
        binding.rvScenes.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvScenes.setAdapter(sceneAdapter);
        sceneAdapter.setOnSceneExecuteListener(scene -> {
            viewModel.executeScene(scene);
        });

        // 观察数据
        viewModel.getDevices().observe(getViewLifecycleOwner(), devices -> {
            deviceAdapter.setData(devices);
            if (devices == null || devices.isEmpty()) {
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
        viewModel.loadDevices(currentGreenhouseId);
        viewModel.loadScenes(currentGreenhouseId);

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
            viewModel.loadDevices(greenhouseId);
            viewModel.loadScenes(greenhouseId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
