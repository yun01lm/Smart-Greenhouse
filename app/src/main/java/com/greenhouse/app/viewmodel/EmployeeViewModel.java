package com.greenhouse.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.greenhouse.app.data.model.AddEmployeeRequest;
import com.greenhouse.app.data.model.EmployeeItem;
import com.greenhouse.app.data.model.EmployeePermissionItem;
import com.greenhouse.app.data.model.UpdatePermissionRequest;
import com.greenhouse.app.data.repository.EmployeeRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 员工管理 ViewModel（棚主端，R26）
 * <p>管理员工列表加载、新增、重置密码、权限更新、移除。业务逻辑全部在 ViewModel。</p>
 */
public class EmployeeViewModel extends ViewModel {

    private final EmployeeRepository repository;

    private final MutableLiveData<List<EmployeeItem>> employees = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public EmployeeViewModel() {
        this.repository = new EmployeeRepository();
    }

    public LiveData<List<EmployeeItem>> getEmployees() { return employees; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }

    /** 加载员工列表 */
    public void loadEmployees() {
        isLoading.setValue(true);
        repository.getEmployees(new EmployeeRepository.Callback<List<EmployeeItem>>() {
            @Override
            public void onSuccess(List<EmployeeItem> data) {
                employees.setValue(data != null ? data : new ArrayList<>());
                isLoading.setValue(false);
            }

            @Override
            public void onError(String error) {
                message.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    /** 新增员工 */
    public void addEmployee(AddEmployeeRequest request) {
        isLoading.setValue(true);
        repository.addEmployee(request, new EmployeeRepository.Callback<EmployeePermissionItem>() {
            @Override
            public void onSuccess(EmployeePermissionItem data) {
                message.setValue("员工添加成功");
                isLoading.setValue(false);
                loadEmployees();
            }

            @Override
            public void onError(String error) {
                message.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    /** 加载员工权限（供权限设置对话框使用，回调返回权限列表） */
    public void loadPermissions(long employeeId, EmployeeRepository.Callback<List<EmployeePermissionItem>> callback) {
        repository.getEmployeePermissions(employeeId, callback);
    }

    /** 重置员工密码 */
    public void resetPassword(long employeeId, String newPassword) {
        isLoading.setValue(true);
        repository.resetPassword(employeeId, newPassword, new EmployeeRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                message.setValue("密码重置成功");
                isLoading.setValue(false);
            }

            @Override
            public void onError(String error) {
                message.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    /** 保存权限（循环提交该员工各大棚权限） */
    public void savePermissions(long employeeId, List<EmployeePermissionItem> permissions) {
        isLoading.setValue(true);
        submitPermission(employeeId, permissions, 0);
    }

    private void submitPermission(long employeeId, List<EmployeePermissionItem> permissions, int index) {
        if (index >= permissions.size()) {
            message.setValue("权限保存成功");
            isLoading.setValue(false);
            return;
        }
        EmployeePermissionItem p = permissions.get(index);
        repository.updateEmployeePermission(employeeId, new UpdatePermissionRequest(p),
                new EmployeeRepository.Callback<EmployeePermissionItem>() {
                    @Override
                    public void onSuccess(EmployeePermissionItem data) {
                        submitPermission(employeeId, permissions, index + 1);
                    }

                    @Override
                    public void onError(String error) {
                        message.setValue("权限保存失败: " + error);
                        isLoading.setValue(false);
                    }
                });
    }

    /** 移除员工 */
    public void removeEmployee(long employeeId) {
        isLoading.setValue(true);
        repository.removeEmployee(employeeId, new EmployeeRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                message.setValue("员工已移除");
                isLoading.setValue(false);
                loadEmployees();
            }

            @Override
            public void onError(String error) {
                message.setValue(error);
                isLoading.setValue(false);
            }
        });
    }

    /** 清除一次性消息（避免重复 Toast） */
    public void consumeMessage() {
        message.setValue(null);
    }
}