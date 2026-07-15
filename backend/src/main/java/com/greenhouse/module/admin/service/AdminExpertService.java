package com.greenhouse.module.admin.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.DataAuthorization;
import com.greenhouse.entity.ExpertAvailability;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理员专家工作台服务
 * <p>
 * 提供全量专家列表、在线状态管理、授权记录查询和统计功能。
 * 与 ExpertService 的区别：ADMIN 可查看所有专家和授权，不限制用户范围。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExpertService {

    private final UserRepository userRepository;
    private final ExpertAvailabilityRepository availabilityRepository;
    private final DataAuthorizationRepository authorizationRepository;
    private final ChatConversationRepository conversationRepository;
    private final GreenhouseRepository greenhouseRepository;

    /**
     * 获取专家列表（含在线状态和咨询数）
     */
    public List<Map<String, Object>> listExperts() {
        List<User> experts = userRepository.findByRole(User.Role.EXPERT);
        List<Map<String, Object>> result = new ArrayList<>();

        for (User expert : experts) {
            ExpertAvailability av = availabilityRepository.findByExpertId(expert.getId())
                    .orElse(ExpertAvailability.builder()
                            .expertId(expert.getId()).isOnline(0).maxConcurrent(5).build());

            long consultCount = conversationRepository.countByExpertId(expert.getId());

            result.add(Map.of(
                    "id", expert.getId(),
                    "name", expert.getUsername(),
                    "phone", expert.getPhone() != null ? expert.getPhone() : "",
                    "isOnline", av.getIsOnline(),
                    "maxConcurrent", av.getMaxConcurrent(),
                    "lastActiveAt", av.getLastActiveAt() != null
                            ? av.getLastActiveAt().toString() : "",
                    "consultCount", consultCount,
                    "status", expert.getStatus()
            ));
        }
        return result;
    }

    /**
     * 切换专家在线状态
     */
    @Transactional
    public Map<String, Object> toggleOnline(Long expertId, boolean isOnline) {
        User expert = userRepository.findById(expertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "专家不存在"));
        if (expert.getRole() != User.Role.EXPERT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该用户不是专家");
        }

        ExpertAvailability av = availabilityRepository.findByExpertId(expertId)
                .orElse(ExpertAvailability.builder()
                        .expertId(expertId).maxConcurrent(5).build());

        av.setIsOnline(isOnline ? 1 : 0);
        av.setLastActiveAt(LocalDateTime.now());
        availabilityRepository.save(av);

        log.info("[ADMIN] 专家在线状态已切换: expertId={}, online={}", expertId, isOnline);

        return Map.of("id", expertId, "isOnline", isOnline);
    }

    /**
     * 全量授权记录（分页）
     */
    public Page<Map<String, Object>> listAuthorizations(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<DataAuthorization> authPage;

        if (status != null && !status.isEmpty()) {
            DataAuthorization.AuthorizationStatus s =
                    DataAuthorization.AuthorizationStatus.valueOf(status.toUpperCase());
            authPage = authorizationRepository.findByStatus(s, pageable);
        } else {
            authPage = authorizationRepository.findAll(pageable);
        }

        return authPage.map(auth -> {
            User expert = userRepository.findById(auth.getExpertId()).orElse(null);
            User user = userRepository.findById(auth.getUserId()).orElse(null);
            Greenhouse gh = greenhouseRepository.findById(auth.getGreenhouseId()).orElse(null);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", auth.getId());
            map.put("expertName", expert != null ? expert.getUsername() : "未知");
            map.put("userName", user != null ? user.getUsername() : "未知");
            map.put("greenhouseName", gh != null ? gh.getName() : "未知");
            map.put("status", auth.getStatus().name());
            map.put("reason", auth.getReason());
            map.put("requestedAt", auth.getRequestedAt());
            map.put("approvedAt", auth.getApprovedAt());
            map.put("expiresAt", auth.getExpiresAt());
            // 计算剩余天数
            long remaining = 0;
            if (auth.getExpiresAt() != null &&
                    auth.getStatus() == DataAuthorization.AuthorizationStatus.APPROVED) {
                remaining = java.time.Duration.between(LocalDateTime.now(), auth.getExpiresAt()).toDays();
                if (remaining < 0) remaining = 0;
            }
            map.put("remainingDays", remaining);
            return map;
        });
    }

    /**
     * 专家工作台统计数据
     */
    public Map<String, Object> getStats() {
        List<User> experts = userRepository.findByRole(User.Role.EXPERT);
        long onlineCount = availabilityRepository.countByIsOnline(1);
        long authTotal = authorizationRepository.count();
        long convTotal = conversationRepository.count();

        return Map.of(
                "expertTotal", experts.size(),
                "onlineCount", onlineCount,
                "authTotal", authTotal,
                "convTotal", convTotal
        );
    }
}
