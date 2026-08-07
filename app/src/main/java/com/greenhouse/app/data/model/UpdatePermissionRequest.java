package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 更新员工权限请求（棚主端，R26）
 */
public class UpdatePermissionRequest {

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("canViewData")
    private boolean canViewData;

    @SerializedName("canControlDevice")
    private boolean canControlDevice;

    @SerializedName("canDiagnose")
    private boolean canDiagnose;

    @SerializedName("canAskExpert")
    private boolean canAskExpert;

    @SerializedName("canViewAlerts")
    private boolean canViewAlerts;

    @SerializedName("canViewHistory")
    private boolean canViewHistory;

    public UpdatePermissionRequest() {}

    public UpdatePermissionRequest(EmployeePermissionItem p) {
        this.greenhouseId = p.getGreenhouseId();
        this.canViewData = p.isCanViewData();
        this.canControlDevice = p.isCanControlDevice();
        this.canDiagnose = p.isCanDiagnose();
        this.canAskExpert = p.isCanAskExpert();
        this.canViewAlerts = p.isCanViewAlerts();
        this.canViewHistory = p.isCanViewHistory();
    }

    public long getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(long greenhouseId) { this.greenhouseId = greenhouseId; }
    public boolean isCanViewData() { return canViewData; }
    public void setCanViewData(boolean v) { canViewData = v; }
    public boolean isCanControlDevice() { return canControlDevice; }
    public void setCanControlDevice(boolean v) { canControlDevice = v; }
    public boolean isCanDiagnose() { return canDiagnose; }
    public void setCanDiagnose(boolean v) { canDiagnose = v; }
    public boolean isCanAskExpert() { return canAskExpert; }
    public void setCanAskExpert(boolean v) { canAskExpert = v; }
    public boolean isCanViewAlerts() { return canViewAlerts; }
    public void setCanViewAlerts(boolean v) { canViewAlerts = v; }
    public boolean isCanViewHistory() { return canViewHistory; }
    public void setCanViewHistory(boolean v) { canViewHistory = v; }
}