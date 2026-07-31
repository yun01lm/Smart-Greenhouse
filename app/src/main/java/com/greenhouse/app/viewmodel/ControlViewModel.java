package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.ActuatorInfo;
import com.greenhouse.app.data.model.ControlResponse;
import com.greenhouse.app.data.model.SceneInfo;
import com.greenhouse.app.data.repository.ControlRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备控制 ViewModel
 * <p>
 * 管理设备列表、场景列表、控制指令下发。
 * 符合规范：ViewModel 不持有 Context，所有网络请求在 Repository 子线程执行。
 * </p>
 */
public class ControlViewModel extends ViewModel {

    private final ControlRepository repository;

    // 设备列表
    private final MutableLiveData<List<ActuatorInfo>> devices = new MutableLiveData<>(new ArrayList<>());
    // 场景列表
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

    public LiveData<List<ActuatorInfo>> getDevices() { return devices; }
    public LiveData<List<SceneInfo>> getScenes() { return scenes; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getActionResult() { return actionResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void setCurrentGreenhouseId(long id) { this.currentGreenhouseId = id; }

    // ===== 加载设备列表 =====

    public void loadDevices(long greenhouseId) {
        this.currentGreenhouseId = greenhouseId;
        isLoading.setValue(true);
        repository.getActuators(greenhouseId, new ControlRepository.Callback<List<ActuatorInfo>>() {
            @Override
            public void onSuccess(List<ActuatorInfo> data) {
                isLoading.postValue(false);
                devices.postValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("加载设备失败: " + message);
            }
        });
    }

    // ===== 加载场景列表 =====

    public void loadScenes(long greenhouseId) {
        repository.getScenes(greenhouseId, new ControlRepository.Callback<List<SceneInfo>>() {
            @Override
            public void onSuccess(List<SceneInfo> data) {
                scenes.postValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("加载场景失败: " + message);
            }
        });
    }

    // ===== 单个设备控制 =====

    public void controlActuator(ActuatorInfo device, String action) {
        isLoading.setValue(true);
        repository.controlActuator(device.getId(), action, currentGreenhouseId,
                new ControlRepository.Callback<ControlResponse>() {
                    @Override
                    public void onSuccess(ControlResponse data) {
                        isLoading.postValue(false);
                        actionResult.postValue(data.getResultText());
                        // 刷新设备列表
                        loadDevices(currentGreenhouseId);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        actionResult.postValue("控制失败: " + message);
                    }
                });
    }

    // ===== 场景执行 =====

    public void executeScene(SceneInfo scene) {
        isLoading.setValue(true);
        actionResult.postValue("正在执行场景「" + scene.getName() + "」...");
        repository.executeScene(scene.getId(), currentGreenhouseId,
                new ControlRepository.Callback<ControlResponse>() {
                    @Override
                    public void onSuccess(ControlResponse data) {
                        isLoading.postValue(false);
                        // 汇总场景执行结果
                        if (data.getResults() != null) {
                            int successCount = 0;
                            int failCount = 0;
                            for (ControlResponse.ActionResult r : data.getResults()) {
                                if (r.isSuccess()) successCount++;
                                else failCount++;
                            }
                            String result = "场景「" + scene.getName() + "」执行完成: "
                                    + successCount + "成功";
                            if (failCount > 0) result += ", " + failCount + "失败";
                            actionResult.postValue(result);
                        } else {
                            actionResult.postValue("场景「" + scene.getName() + "」已执行");
                        }
                        // 刷新设备列表
                        loadDevices(currentGreenhouseId);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        actionResult.postValue("场景执行失败: " + message);
                    }
                });
    }
}
