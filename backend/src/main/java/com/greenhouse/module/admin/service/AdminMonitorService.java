package com.greenhouse.module.admin.service;

import com.greenhouse.entity.Alert;
import com.greenhouse.entity.Device;
import com.greenhouse.module.admin.dto.MonitorOverviewResponse;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;

/**
 * 管理员系统监控服务
 * <p>
 * 聚合 4 类监控数据：设备在线率、告警统计、服务连接状态、系统数据概览。
 * 设计决策（记录于 TASK-G07.md）：
 * - 选择自建监控端点而非引入 Spring Boot Actuator。原因：当前监控需求为业务级别
 *   （设备在线率/告警统计/系统概览），Actuator 擅长的是 JVM 技术指标，两者互补但不重叠。
 *   自建端点零依赖、数据格式定制化、前端直接可用。后续如需技术指标监控再引入 Actuator。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMonitorService {

    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final GreenhouseRepository greenhouseRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final MqttClient mqttClient;
    private final DataSource dataSource;

    /**
     * 获取综合监控概览
     */
    public MonitorOverviewResponse getOverview() {
        return MonitorOverviewResponse.builder()
                .deviceStats(buildDeviceStats())
                .alertStats(buildAlertStats())
                .serviceStatus(buildServiceStatus())
                .systemOverview(buildSystemOverview())
                .build();
    }

    // ===== 1. 设备在线率 =====

    private MonitorOverviewResponse.DeviceStats buildDeviceStats() {
        long total = deviceRepository.count();
        long online = deviceRepository.countByStatus(Device.DeviceStatus.ONLINE);
        long offline = deviceRepository.countByStatus(Device.DeviceStatus.OFFLINE);
        long alarm = deviceRepository.countByStatus(Device.DeviceStatus.ALARM);

        return MonitorOverviewResponse.DeviceStats.builder()
                .total(total)
                .online(online)
                .offline(offline)
                .alarm(alarm)
                .build();
    }

    // ===== 2. 告警统计（最近 24 小时） =====

    private MonitorOverviewResponse.AlertStats buildAlertStats() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        // 使用 JPA 全量查询后内存过滤（告警表数据量可控）
        var allAlerts = alertRepository.findAll();
        var recentAlerts = allAlerts.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(since))
                .toList();

        long info = recentAlerts.stream()
                .filter(a -> a.getLevel() == Alert.AlertLevel.INFO).count();
        long warning = recentAlerts.stream()
                .filter(a -> a.getLevel() == Alert.AlertLevel.WARNING).count();
        long critical = recentAlerts.stream()
                .filter(a -> a.getLevel() == Alert.AlertLevel.CRITICAL).count();

        return MonitorOverviewResponse.AlertStats.builder()
                .total24h(recentAlerts.size())
                .info(info)
                .warning(warning)
                .critical(critical)
                .build();
    }

    // ===== 3. 服务连接状态 =====

    private MonitorOverviewResponse.ServiceStatus buildServiceStatus() {
        boolean mqttOk = mqttClient.isConnected();
        boolean dbOk = checkDatabase();

        return MonitorOverviewResponse.ServiceStatus.builder()
                .mqtt(mqttOk)
                .database(dbOk)
                .build();
    }

    private boolean checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(3);
        } catch (Exception e) {
            log.warn("数据库连接检查失败", e);
            return false;
        }
    }

    // ===== 4. 系统数据概览 =====

    private MonitorOverviewResponse.SystemOverview buildSystemOverview() {
        return MonitorOverviewResponse.SystemOverview.builder()
                .greenhouses(greenhouseRepository.count())
                .devices(deviceRepository.count())
                .users(userRepository.count())
                .rules(alertRuleRepository.count())
                .build();
    }
}
