package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 员工权限模型（棚主端，R26）
 * <p>对应后端 PermissionResponse：员工对某个大棚的 6 项功能权限。</p>
 */
public class EmployeePermissionItem {

    private long id;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("greenhouseName")
    private String greenhouseName;

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

    public long getId() { return id; }
    public long getGreenhouseId() { return greenhouseId; }
    public String getGreenhouseName() { return greenhouseName; }

    public void setId(long id) { this.id = id; }
    public void setGreenhouseId(long greenhouseId) { this.greenhouseId = greenhouseId; }
    public void setGreenhouseName(String greenhouseName) { this.greenhouseName = greenhouseName; }
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