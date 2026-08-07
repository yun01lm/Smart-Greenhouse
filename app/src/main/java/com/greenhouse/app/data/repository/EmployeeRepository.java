package com.greenhouse.app.data.repository;

import com.greenhouse.app.data.model.AddEmployeeRequest;
import com.greenhouse.app.data.model.ApiResponse;
import com.greenhouse.app.data.model.EmployeeItem;
import com.greenhouse.app.data.model.EmployeePermissionItem;
import com.greenhouse.app.data.model.ResetPasswordRequest;
import com.greenhouse.app.data.model.UpdatePermissionRequest;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;

/**
 * 员工管理数据仓库（棚主端，R26）
 * <p>封装员工列表、新增员工（创建/邀请）、重置密码、权限查询/更新、移除员工。</p>
 */
public class EmployeeRepository extends BaseRepository {

    /** 员工列表 */
    public void getEmployees(Callback<List<EmployeeItem>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<EmployeeItem>>> response = apiService.getEmployees().execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    /** 新增员工（创建或邀请） */
    public void addEmployee(AddEmployeeRequest request, Callback<EmployeePermissionItem> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<EmployeePermissionItem>> response =
                        apiService.addEmployee(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    /** 重置员工密码 */
    public void resetPassword(long employeeId, String newPassword, Callback<Void> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<Void>> response =
                        apiService.resetEmployeePassword(employeeId, new ResetPasswordRequest(newPassword)).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, null);
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    /** 员工权限列表 */
    public void getEmployeePermissions(long employeeId, Callback<List<EmployeePermissionItem>> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<List<EmployeePermissionItem>>> response =
                        apiService.getEmployeePermissions(employeeId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    /** 更新员工权限 */
    public void updateEmployeePermission(long employeeId, UpdatePermissionRequest request,
                                         Callback<EmployeePermissionItem> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<EmployeePermissionItem>> response =
                        apiService.updateEmployeePermission(employeeId, request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, response.body().getData());
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }

    /** 移除员工（解除归属 + 删除权限记录） */
    public void removeEmployee(long employeeId, Callback<Void> callback) {
        execute(() -> {
            try {
                Response<ApiResponse<Void>> response = apiService.removeEmployee(employeeId).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    postSuccess(callback, null);
                } else {
                    postError(callback, parseError(response));
                }
            } catch (IOException e) {
                postError(callback, "网络异常: " + e.getMessage());
            }
        });
    }
}