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
 * <p>
 * 并发防护（修复重复点击卡死/空白）：
 * 1) 加载采用"代际计数"，只接受最新一轮结果，旧响应丢弃，避免交错覆盖；
 * 2) loading 用计数管理，全部加载结束才隐藏；
 * 3) 场景执行/创建防重，操作进行中忽略重复提交。
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
    // 是否有操作进行中（场景执行/创建等耗时操作，用于禁用重复点击）
    private final MutableLiveData<Boolean> isOperating = new MutableLiveData<>(false);

    private long currentGreenhouseId;

    // 加载代际计数：只接受最新一轮加载结果
    private int deviceLoadGeneration = 0;
    private int sceneLoadGeneration = 0;
    // 并发加载计数：全部结束才隐藏 loading
    private int loadingCount = 0;

    public ControlViewModel() {
        this.repository = new ControlRepository();
    }

    // ===== LiveData =====

    public LiveData<List<DeviceGroup>> getDeviceGroups() { return deviceGroups; }
    public LiveData<List<SceneInfo>> getScenes() { return scenes; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getActionResult() { return actionResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsOperating() { return isOperating; }

    public void setCurrentGreenhouseId(long id) { this.currentGreenhouseId = id; }

    // ===== 加载状态管理 =====

    private void beginLoading() {
        loadingCount++;
        isLoading.setValue(true);
    }

    private void endLoading() {
        loadingCount = Math.max(0, loadingCount - 1);
        if (loadingCount == 0) {
            isLoading.setValue(false);
        }
    }

    // ===== 按大棚分组加载设备 =====

    public void loadDeviceGroups() {
        final int generation = ++deviceLoadGeneration;
        beginLoading();
        repository.getGreenhouses(new ControlRepository.Callback<List<Greenhouse>>() {
            @Override
            public void onSuccess(List<Greenhouse> greenhouses) {
                if (generation != deviceLoadGeneration) return; // 过期响应丢弃
                if (greenhouses == null || greenhouses.isEmpty()) {
                    endLoading();
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
                                    addGroup(generation, groups, remaining, gh.getId(), gh.getName(),
                                            data != null ? data : new ArrayList<>());
                                }

                                @Override
                                public void onError(String message) {
                                    addGroup(generation, groups, remaining, gh.getId(), gh.getName(),
                                            new ArrayList<>());
                                }
                            });
                }
            }

            @Override
            public void onError(String message) {
                if (generation != deviceLoadGeneration) return;
                endLoading();
                errorMessage.postValue("加载大棚失败: " + message);
            }
        });
    }

    private void addGroup(int generation, List<DeviceGroup> groups, int[] remaining,
                          long ghId, String ghName, List<DeviceInfo> devices) {
        synchronized (remaining) {
            groups.add(new DeviceGroup(ghId, ghName, devices));
            remaining[0]--;
            if (remaining[0] == 0) {
                if (generation != deviceLoadGeneration) return; // 过期响应丢弃
                List<DeviceGroup> sorted = new ArrayList<>(groups);
                sorted.sort(Comparator.comparingLong(DeviceGroup::getGreenhouseId));
                deviceGroups.postValue(sorted);
                endLoading();
            }
        }
    }

    // ===== 加载全部大棚场景 =====

    public void loadAllScenes() {
        final int generation = ++sceneLoadGeneration;
        beginLoading();
        repository.getGreenhouses(new ControlRepository.Callback<List<Greenhouse>>() {
            @Override
            public void onSuccess(List<Greenhouse> greenhouses) {
                if (generation != sceneLoadGeneration) return;
                if (greenhouses == null || greenhouses.isEmpty()) {
                    scenes.postValue(new ArrayList<>());
                    endLoading();
                    return;
                }
                List<SceneInfo> all = Collections.synchronizedList(new ArrayList<>());
                int[] remaining = {greenhouses.size()};
                for (Greenhouse gh : greenhouses) {
                    repository.getScenes(gh.getId(), new ControlRepository.Callback<List<SceneInfo>>() {
                        @Override
                        public void onSuccess(List<SceneInfo> data) {
                            collectScenes(generation, all, remaining, data);
                        }

                        @Override
                        public void onError(String message) {
                            collectScenes(generation, all, remaining, null);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (generation != sceneLoadGeneration) return;
                endLoading();
                errorMessage.postValue("加载场景失败: " + message);
            }
        });
    }

    private void collectScenes(int generation, List<SceneInfo> all, int[] remaining, List<SceneInfo> data) {
        synchronized (remaining) {
            if (data != null) all.addAll(data);
            remaining[0]--;
            if (remaining[0] == 0) {
                if (generation != sceneLoadGeneration) return;
                List<SceneInfo> sorted = new ArrayList<>(all);
                sorted.sort(Comparator.comparingLong(SceneInfo::getId));
                scenes.postValue(sorted);
                endLoading();
            }
        }
    }

    // ===== 单个设备控制 =====

    public void controlActuator(DeviceInfo device, String action) {
        repository.controlActuator(device.getId(), action,
                new ControlRepository.Callback<DeviceControlResult>() {
                    @Override
                    public void onSuccess(DeviceControlResult data) {
                        actionResult.postValue(data != null ? data.getResultText() : "控制指令已下发");
                        // 本地即时更新设备开关状态（避免整页等待刷新）
                        updateDeviceLocalState(device.getId(), "ON".equals(action));
                        // 后台静默刷新（代际计数保证只应用最新结果）
                        loadDeviceGroups();
                        loadAllScenes();
                    }

                    @Override
                    public void onError(String message) {
                        actionResult.postValue("控制失败: " + message);
                    }
                });
    }

    /**
     * 本地即时更新设备状态（控制成功后先改 UI 再后台刷新）
     */
    private void updateDeviceLocalState(long deviceId, boolean on) {
        List<DeviceGroup> current = deviceGroups.getValue();
        if (current == null) return;
        boolean changed = false;
        for (DeviceGroup group : current) {
            if (group.getDevices() == null) continue;
            for (DeviceInfo device : group.getDevices()) {
                if (device.getId() != null && device.getId() == deviceId) {
                    device.setLastValue(on ? "ON" : "OFF");
                    device.setStatus("ONLINE");
                    changed = true;
                }
            }
        }
        if (changed) {
            deviceGroups.postValue(current);
        }
    }

    // ===== 创建场景（防重） =====

    public void createScene(long greenhouseId, String name, String description,
                            List<CreateSceneRequest.SceneActionItem> actions) {
        if (Boolean.TRUE.equals(isOperating.getValue())) {
            actionResult.postValue("有操作正在进行，请稍候");
            return;
        }
        isOperating.setValue(true);
        CreateSceneRequest request = new CreateSceneRequest(name, description, actions);
        repository.createScene(greenhouseId, request, new ControlRepository.Callback<SceneInfo>() {
            @Override
            public void onSuccess(SceneInfo data) {
                isOperating.postValue(false);
                String sceneName = (data != null && data.getName() != null) ? data.getName() : name;
                actionResult.postValue("场景「" + sceneName + "」创建成功");
                loadAllScenes();
            }

            @Override
            public void onError(String message) {
                isOperating.postValue(false);
                actionResult.postValue("创建场景失败: " + message);
            }
        });
    }

    // ===== 场景执行（防重） =====

    public void executeScene(SceneInfo scene) {
        if (Boolean.TRUE.equals(isOperating.getValue())) {
            actionResult.postValue("场景执行中，请稍候");
            return;
        }
        isOperating.setValue(true);
        actionResult.postValue("正在执行场景「" + scene.getName() + "」...");
        repository.executeScene(scene.getId(),
                new ControlRepository.Callback<List<DeviceControlResult>>() {
                    @Override
                    public void onSuccess(List<DeviceControlResult> data) {
                        isOperating.postValue(false);
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
                        isOperating.postValue(false);
                        actionResult.postValue("场景执行失败: " + message);
                    }
                });
    }
}
