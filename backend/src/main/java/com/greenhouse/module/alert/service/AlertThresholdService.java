package com.greenhouse.module.alert.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.UserAlertThreshold;
import com.greenhouse.module.alert.dto.ThresholdRequest;
import com.greenhouse.module.alert.dto.ThresholdResponse;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserAlertThresholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户自定义预警阈值服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertThresholdService {

    private final UserAlertThresholdRepository thresholdRepository;
    private final GreenhouseRepository greenhouseRepository;

    /**
     * 设置自定义阈值（创建或更新）
     */
    @Transactional
    public ThresholdResponse setThreshold(Long userId, ThresholdRequest request) {
        Greenhouse greenhouse = greenhouseRepository.findById(request.getGreenhouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GREENHOUSE_NOT_FOUND));

        // 查找是否已有同传感器类型的阈值
        Optional<UserAlertThreshold> existing = thresholdRepository
                .findByUserIdAndGreenhouseIdAndSensorType(
                        userId, request.getGreenhouseId(), request.getSensorType());

        UserAlertThreshold threshold;
        if (existing.isPresent()) {
            // 更新
            threshold = existing.get();
            threshold.setMinThreshold(request.getMinThreshold());
            threshold.setMaxThreshold(request.getMaxThreshold());
            threshold.setGroupId(request.getGroupId());
            threshold.setEnabled(request.getEnabled());
            log.info("自定义阈值已更新: userId={}, greenhouseId={}, sensorType={}",
                    userId, request.getGreenhouseId(), request.getSensorType());
        } else {
            // 创建
            threshold = UserAlertThreshold.builder()
                    .userId(userId)
                    .greenhouseId(request.getGreenhouseId())
                    .groupId(request.getGroupId())
                    .sensorType(request.getSensorType())
                    .minThreshold(request.getMinThreshold())
                    .maxThreshold(request.getMaxThreshold())
                    .enabled(request.getEnabled())
                    .build();
            log.info("自定义阈值已创建: userId={}, greenhouseId={}, sensorType={}",
                    userId, request.getGreenhouseId(), request.getSensorType());
        }

        threshold = thresholdRepository.save(threshold);
        return ThresholdResponse.fromEntity(threshold);
    }

    /**
     * 查询用户自定义阈值列表
     */
    public List<ThresholdResponse> listThresholds(Long userId, Long greenhouseId) {
        List<UserAlertThreshold> thresholds;
        if (greenhouseId != null) {
            thresholds = thresholdRepository.findByUserIdAndGreenhouseId(userId, greenhouseId);
        } else {
            thresholds = thresholdRepository.findByUserId(userId);
        }
        return thresholds.stream().map(ThresholdResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * 删除自定义阈值
     */
    @Transactional
    public void deleteThreshold(Long userId, Long thresholdId) {
        UserAlertThreshold threshold = thresholdRepository.findById(thresholdId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "阈值记录不存在"));

        if (!threshold.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        thresholdRepository.delete(threshold);
        log.info("自定义阈值已删除: id={}", thresholdId);
    }
}
