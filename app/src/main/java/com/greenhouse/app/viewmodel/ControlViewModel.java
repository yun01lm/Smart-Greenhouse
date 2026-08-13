package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.DeviceControlResult;
import com.greenhouse.app.data.model.DeviceGroup;
import com.greenhouse.app.data.model.DeviceInfo;
import com.greenhouse.app.data.model.CreateSceneRequest;
import com.greenhouse.app.data.model.Greenhouse;
import com.greenhouse.app.data.model.SceneInfo;
import com.greenhouse.app.data.repository.ControlRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 设备控制 ViewModel
 * <p>
 * 按大棚分组加载设备列表、加载全部大棚场景、创建场景、下发控制指令。
 * 符合规范：ViewModel 不持有 Context，所有网络请求在 Repository 子线程执行。
 * </p>
 */
public class ControlViewModel extends ViewModel {

    private final ControlRepository repository;

    // 按大棚分组的设备列表
    private final MutableLiveData<List<DeviceGroup>> deviceGroups = new MutableLiveData<>(new ArrayList<>());
    // 场景列表（全部大棚）
    private final MutableLiveData<List<SceneInfo>> scenes = new MutableLiveData<>(new ArrayList<>());
    // 加载状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // 操作结果反馈
    private final MutableLiveData<String> actionResult = new MutableLiveData<>();
    // 错误信息
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long currentGreenhouseId;

    public ControlViewModel() {
        this.repository = new ControlRepository();
    }

    // ===== LiveData =====

    public LiveData<List<DeviceGroup>> getDeviceGroups() { return deviceGroups; }
    public LiveData<List<SceneInfo>> getScenes() { return scenes; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getActionResult() { return actionResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void setCurrentGreenhouseId(long id) { this.currentGreenhouseId = id; }

    // ===== 按大棚分组加载设备 =====

    public void loadDeviceGroups() {
        isLoading.setValue(true);
        repository.getGreenhouses(new ControlRepository.Callback<List<Greenhouse>>() {
            @Override
            public void onSuccess(List<Greenhouse> greenhouses) {
                if (greenhouses == null || greenhouses.isEmpty()) {
                    isLoading.postValue(false);
                    deviceGroups.postValue(new ArrayList<>());
                    return;
                }
                List<DeviceGroup> groups = Collections.synchronizedList(new ArrayList<>());
                int[] remaining = {greenhouses.size()};
                for (Greenhouse gh : greenhouses) {
                    repository.getDevices(gh.getId(),
                            new ControlRepository.Callback<List<DeviceInfo>>() {
                                @Override
                                public void onSuccess(List<DeviceInfo> data) {
                                    addGroup(groups, remaining, gh.getId(), gh.getName(),
                                            data != null ? data : new ArrayList<>());
                                }

                                @Override
                                public void onError(String message) {
                                    addGroup(groups, remaining, gh.getId(), gh.getName(), new ArrayList<>());
                                }
                            });
                }
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("加载大棚失败: " + message);
            }
        });
    }

    private void addGroup(List<DeviceGroup> groups, int[] remaining,
                          long ghId, String ghName, List<DeviceInfo> devices) {
        synchronized (remaining) {
            groups.add(new DeviceGroup(ghId, ghName, devices));
            remaining[0]--;
            if (remaining[0] == 0) {
                List<DeviceGroup> sorted = new ArrayList<>(groups);
                sorted.sort(Comparator.comparingLong(DeviceGroup::getGreenhouseId));
                deviceGroups.postValue(sorted);
                isLoading.postValue(false);
            }
        }
    }

    // ===== 加载全部大棚场景 =====

    public void loadAllScenes() {
        repository.getGreenhouses(new ControlRepository.Callback<List<Greenhouse>>() {
            @Override
            public void onSuccess(List<Greenhouse> greenhouses) {
                if (greenhouses == null || greenhouses.isEmpty()) {
                    scenes.postValue(new ArrayList<>());
                    return;
                }
                List<SceneInfo> all = Collections.synchronizedList(new ArrayList<>());
                int[] remaining = {greenhouses.size()};
                for (Greenhouse gh : greenhouses) {
                    repository.getScenes(gh.getId(), new ControlRepository.Callback<List<SceneInfo>>() {
                        @Override
                        public void onSuccess(List<SceneInfo> data) {
                            collectScenes(all, remaining, data);
                        }

                        @Override
                        public void onError(String message) {
                            collectScenes(all, remaining, null);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("加载场景失败: " + message);
            }
        });
    }

    private void collectScenes(List<SceneInfo> all, int[] remaining, List<SceneInfo> data) {
        synchronized (remaining) {
            if (data != null) all.addAll(data);
            remaining[0]--;
            if (remaining[0] == 0) {
                List<SceneInfo> sorted = new ArrayList<>(all);
                sorted.sort(Comparator.comparingLong(SceneInfo::getId));
                scenes.postValue(sorted);
            }
        }
    }

    // ===== 单个设备控制 =====

    public void controlActuator(DeviceInfo device, String action) {
        isLoading.setValue(true);
        repository.controlActuator(device.getId(), action,
                new ControlRepository.Callback<DeviceControlResult>() {
                    @Override
                    public void onSuccess(DeviceControlResult data) {
                        isLoading.postValue(false);
                        actionResult.postValue(data != null ? data.getResultText() : "控制指令已下发");
                        // 刷新设备分组（状态/开关会变化）
                        loadDeviceGroups();
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        actionResult.postValue("控制失败: " + message);
                    }
                });
    }

    // ===== 创建场景 =====

    public void createScene(long greenhouseId, String name, String description,
                            List<CreateSceneRequest.SceneActionItem> actions) {
        isLoading.setValue(true);
        CreateSceneRequest request = new CreateSceneRequest(name, description, actions);
        repository.createScene(greenhouseId, request, new ControlRepository.Callback<SceneInfo>() {
            @Override
            public void onSuccess(SceneInfo data) {
                isLoading.postValue(false);
                String sceneName = (data != null && data.getName() != null) ? data.getName() : name;
                actionResult.postValue("场景「" + sceneName + "」创建成功");
                loadAllScenes();
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                actionResult.postValue("创建场景失败: " + message);
            }
        });
    }

    // ===== 场景执行 =====

    public void executeScene(SceneInfo scene) {
        isLoading.setValue(true);
        actionResult.postValue("正在执行场景「" + scene.getName() + "」...");
        repository.executeScene(scene.getId(),
                new ControlRepository.Callback<List<DeviceControlResult>>() {
                    @Override
                    public void onSuccess(List<DeviceControlResult> data) {
                        isLoading.postValue(false);
                        if (data != null && !data.isEmpty()) {
                            int successCount = 0;
                            int failCount = 0;
                            StringBuilder sb = new StringBuilder();
                            for (DeviceControlResult r : data) {
                                if (r.isSuccess()) successCount++;
                                else failCount++;
                                if (sb.length() > 0) sb.append("；");
                                sb.append(r.getResultText());
                            }
                            String result = "场景「" + scene.getName() + "」执行完成: " + successCount + "成功";
                            if (failCount > 0) result += ", " + failCount + "失败";
                            actionResult.postValue(result);
                        } else {
                            actionResult.postValue("场景「" + scene.getName() + "」已执行");
                        }
                        // 刷新设备分组（场景可能改变设备状态）
                        loadDeviceGroups();
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        actionResult.postValue("场景执行失败: " + message);
                    }
                });
    }
}
