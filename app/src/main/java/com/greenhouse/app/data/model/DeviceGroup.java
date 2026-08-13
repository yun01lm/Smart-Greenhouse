package com.greenhouse.app.data.model;

import java.util.List;

/**
 * 设备分组模型（按大棚分组）
 * <p>控制页按大棚展示设备：组标题 = 大棚名 + 设备数。</p>
 */
public class DeviceGroup {

    private long greenhouseId;
    private String greenhouseName;
    private List<DeviceInfo> devices;

    public DeviceGroup(long greenhouseId, String greenhouseName, List<DeviceInfo> devices) {
        this.greenhouseId = greenhouseId;
        this.greenhouseName = greenhouseName;
        this.devices = devices;
    }

    public long getGreenhouseId() { return greenhouseId; }
    public String getGreenhouseName() { return greenhouseName; }
    public List<DeviceInfo> getDevices() { return devices; }

    public int getDeviceCount() {
        return devices == null ? 0 : devices.size();
    }
}