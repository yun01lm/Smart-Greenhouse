package com.greenhouse.module.alert.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.Alert;
import com.greenhouse.module.alert.dto.AlertResponse;
import com.greenhouse.repository.AlertRepository;
import com.greenhouse.repository.GreenhouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 告警记录管理服务
 * <p>
 * 负责告警记录的查询和状态管理。
 * 从 AlertController 中提取，遵循 Controller→Service→Repository 分层约定。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final GreenhouseRepository greenhouseRepository;

    /**
     * 分页查询告警列表
     *
     * @param greenhouseId 大棚ID
     * @param level        告警级别（可选）
     * @param page         页码
     * @param size         每页条数
     * @return 告警分页 + 大棚名称
     */
    public Page<Alert> listAlerts(Long greenhouseId, Alert.AlertLevel level, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        if (level != null) {
            return alertRepository.findByGreenhouseIdAndLevelOrderByCreatedAtDesc(
                    greenhouseId, level, pageable);
        }
        return alertRepository.findByGreenhouseIdOrderByCreatedAtDesc(
                greenhouseId, pageable);
    }

    /**
     * 标记告警为已读
     *
     * @param alertId 告警ID
     * @return 更新后的告警实体
     */
    @Transactional
    public Alert markAsRead(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "告警记录不存在"));
        alert.setReadStatus(true);
        return alertRepository.save(alert);
    }

    /**
     * 标记告警为已处理（同时置为已读）
     *
     * @param alertId 告警ID
     * @return 更新后的告警实体
     */
    @Transactional
    public Alert markAsHandled(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "告警记录不存在"));
        alert.setHandled(true);
        alert.setReadStatus(true);
        return alertRepository.save(alert);
    }

    /**
     * 获取大棚名称
     */
    public String getGreenhouseName(Long greenhouseId) {
        return greenhouseRepository.findById(greenhouseId)
                .map(gh -> gh.getName())
                .orElse("未知大棚");
    }

    /**
     * 获取未读告警数量
     */
    public Map<String, Object> getUnreadCount(Long greenhouseId) {
        long count = alertRepository.countByGreenhouseIdAndReadStatusFalse(greenhouseId);
        return Map.of("count", count);
    }
}
