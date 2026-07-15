package com.greenhouse.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统监控概览响应 DTO
 * <p>
 * 包含 4 类监控数据：设备在线率、告警统计、服务连接状态、系统数据概览。
 * 由 AdminMonitorController 的 /overview 端点一次性返回。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitorOverviewResponse {

    /** 设备统计 */
    private DeviceStats deviceStats;

    /** 告警统计（最近24小时） */
    private AlertStats alertStats;

    /** 服务连接状态 */
    private ServiceStatus serviceStatus;

    /** 系统数据概览 */
    private SystemOverview systemOverview;

    // ===== 内嵌类 =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeviceStats {
        /** 设备总数 */
        private long total;
        /** 在线设备数 */
        private long online;
        /** 离线设备数 */
        private long offline;
        /** 告警状态设备数 */
        private long alarm;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertStats {
        /** 最近24小时告警总数 */
        private long total24h;
        /** 提示级别数量 */
        private long info;
        /** 警告级别数量 */
        private long warning;
        /** 严重级别数量 */
        private long critical;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceStatus {
        /** MQTT Broker 是否已连接 */
        private boolean mqtt;
        /** 数据库是否可连接 */
        private boolean database;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemOverview {
        /** 大棚总数 */
        private long greenhouses;
        /** 设备总数 */
        private long devices;
        /** 用户总数 */
        private long users;
        /** 预警规则总数 */
        private long rules;
    }
}
