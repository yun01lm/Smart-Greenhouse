package com.greenhouse.module.expert.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.*;
import com.greenhouse.module.expert.dto.AuthorizationResponse;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 专家服务
 * <p>
 * 提供专家列表查询、数据授权管理和在线状态管理。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertService {

    private final UserRepository userRepository;
    private final DataAuthorizationRepository authorizationRepository;
    private final ExpertAvailabilityRepository availabilityRepository;
    private final GreenhouseRepository greenhouseRepository;

    /**
     * 获取专家列表
     */
    public List<Map<String, Object>> getExpertList(String specialty, Boolean onlineOnly, int page, int size) {
        List<User> experts = userRepository.findByRole(User.Role.EXPERT);

        List<Map<String, Object>> result = new ArrayList<>();
        for (User expert : experts) {
            ExpertAvailability availability = availabilityRepository
                    .findByExpertId(expert.getId())
                    .orElse(ExpertAvailability.builder()
                            .expertId(expert.getId())
                            .isOnline(0)
                            .maxConcurrent(5)
                            .build());

            // 筛选
            if (onlineOnly != null && onlineOnly && availability.getIsOnline() == 0) {
                continue;
            }

            result.add(Map.of(
                    "id", expert.getId(),
                    "name", expert.getUsername(),
                    "phone", expert.getPhone() != null ? expert.getPhone() : "",
                    "specialty", "农业专家",
                    "isOnline", availability.getIsOnline(),
                    "consultCount", 0  // Phase 4 完善统计
            ));
        }

        return result;
    }

    /**
     * 专家发起授权请求
     */
    @Transactional
    public AuthorizationResponse requestAuthorization(Long expertId, Long userId, Long greenhouseId, String reason) {
        // 检查是否已有有效授权
        Optional<DataAuthorization> existing = authorizationRepository
                .findTopByExpertIdAndUserIdAndGreenhouseIdAndStatusOrderByRequestedAtDesc(
                        expertId, userId, greenhouseId, DataAuthorization.AuthorizationStatus.APPROVED);

        if (existing.isPresent()) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ALREADY_EXISTS);
        }

        DataAuthorization auth = DataAuthorization.builder()
                .expertId(expertId)
                .userId(userId)
                .greenhouseId(greenhouseId)
                .status(DataAuthorization.AuthorizationStatus.PENDING)
                .reason(reason)
                .build();

        auth = authorizationRepository.save(auth);
        log.info("授权请求已发起: expert={}, user={}, greenhouse={}", expertId, userId, greenhouseId);

        User expert = userRepository.findById(expertId).orElse(null);
        Greenhouse greenhouse = greenhouseRepository.findById(greenhouseId).orElse(null);

        return AuthorizationResponse.fromEntity(auth,
                expert != null ? expert.getUsername() : "未知",
                greenhouse != null ? greenhouse.getName() : "未知");
    }

    /**
     * 用户查看待处理的授权请求
     */
    public List<AuthorizationResponse> getPendingAuthorizations(Long userId) {
        List<DataAuthorization> auths = authorizationRepository
                .findByUserIdAndStatusOrderByRequestedAtDesc(userId, DataAuthorization.AuthorizationStatus.PENDING);

        return auths.stream()
                .map(auth -> {
                    User expert = userRepository.findById(auth.getExpertId()).orElse(null);
                    Greenhouse greenhouse = greenhouseRepository.findById(auth.getGreenhouseId()).orElse(null);
                    return AuthorizationResponse.fromEntity(auth,
                            expert != null ? expert.getUsername() : "未知",
                            greenhouse != null ? greenhouse.getName() : "未知");
                })
                .toList();
    }

    /**
     * 用户同意授权
     */
    @Transactional
    public AuthorizationResponse approveAuthorization(Long authId, Long userId) {
        DataAuthorization auth = authorizationRepository.findById(authId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHORIZATION_NOT_FOUND));

        if (!auth.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (auth.getStatus() != DataAuthorization.AuthorizationStatus.PENDING) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        auth.setStatus(DataAuthorization.AuthorizationStatus.APPROVED);
        auth.setApprovedAt(LocalDateTime.now());
        auth.setExpiresAt(LocalDateTime.now().plusDays(7));  // 7天有效期
        auth = authorizationRepository.save(auth);

        log.info("授权已同意: id={}, expires={}", authId, auth.getExpiresAt());

        User expert = userRepository.findById(auth.getExpertId()).orElse(null);
        Greenhouse greenhouse = greenhouseRepository.findById(auth.getGreenhouseId()).orElse(null);
        return AuthorizationResponse.fromEntity(auth,
                expert != null ? expert.getUsername() : "未知",
                greenhouse != null ? greenhouse.getName() : "未知");
    }

    /**
     * 用户拒绝授权
     */
    @Transactional
    public void rejectAuthorization(Long authId, Long userId) {
        DataAuthorization auth = authorizationRepository.findById(authId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHORIZATION_NOT_FOUND));

        if (!auth.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        auth.setStatus(DataAuthorization.AuthorizationStatus.REJECTED);
        authorizationRepository.save(auth);
        log.info("授权已拒绝: id={}", authId);
    }

    /**
     * 用户撤销授权
     */
    @Transactional
    public void revokeAuthorization(Long authId, Long userId) {
        DataAuthorization auth = authorizationRepository.findById(authId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHORIZATION_NOT_FOUND));

        if (!auth.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (auth.getStatus() != DataAuthorization.AuthorizationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        auth.setStatus(DataAuthorization.AuthorizationStatus.REVOKED);
        auth.setRevokedAt(LocalDateTime.now());
        auth.setRevokedBy(userId);
        authorizationRepository.save(auth);
        log.info("授权已撤销: id={}", authId);
    }

    /**
     * 查看有效授权
     */
    public List<AuthorizationResponse> getActiveAuthorizations(Long userId, String role) {
        List<DataAuthorization> auths;

        if ("EXPERT".equalsIgnoreCase(role)) {
            auths = authorizationRepository
                    .findByExpertIdAndStatusOrderByApprovedAtDesc(userId, DataAuthorization.AuthorizationStatus.APPROVED);
        } else {
            auths = authorizationRepository
                    .findByUserIdAndStatusOrderByApprovedAtDesc(userId, DataAuthorization.AuthorizationStatus.APPROVED);
        }

        return auths.stream()
                .map(auth -> {
                    User expert = userRepository.findById(auth.getExpertId()).orElse(null);
                    Greenhouse greenhouse = greenhouseRepository.findById(auth.getGreenhouseId()).orElse(null);
                    return AuthorizationResponse.fromEntity(auth,
                            expert != null ? expert.getUsername() : "未知",
                            greenhouse != null ? greenhouse.getName() : "未知");
                })
                .toList();
    }

    /**
     * 更新专家在线状态
     */
    @Transactional
    public void updateOnlineStatus(Long expertId, boolean isOnline) {
        ExpertAvailability availability = availabilityRepository.findByExpertId(expertId)
                .orElse(ExpertAvailability.builder()
                        .expertId(expertId)
                        .maxConcurrent(5)
                        .build());

        availability.setIsOnline(isOnline ? 1 : 0);
        availability.setLastActiveAt(LocalDateTime.now());
        availabilityRepository.save(availability);

        log.info("专家在线状态已更新: expertId={}, online={}", expertId, isOnline);
    }
}
