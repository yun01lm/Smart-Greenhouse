package com.greenhouse.module.admin.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.Greenhouse;
import com.greenhouse.entity.User;
import com.greenhouse.module.admin.dto.RoleCountResponse;
import com.greenhouse.module.admin.dto.UpdateUserRequest;
import com.greenhouse.module.admin.dto.UserSummaryResponse;
import com.greenhouse.module.greenhouse.service.RegionService;
import com.greenhouse.repository.GreenhouseRepository;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员用户管理服务
 * <p>
 * 仅 ADMIN 角色可调用。提供用户列表查看、详情、更新、删除和角色统计功能。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RegionService regionService;
    private final GreenhouseRepository greenhouseRepository;

    // ===== 用户管理 =====

    /**
     * 用户列表（支持角色 + 五级地区 + 关键词组合筛选）
     * <p>
     * 地区归属推导（与大棚地区字段一致）：
     * - OWNER：其名下大棚所在地区
     * - WORKER：其所属棚主（ownerId）名下大棚所在地区
     * - ADMIN / EXPERT：无大棚地区属性，启用地区筛选时不会出现在结果中
     * </p>
     */
    public List<UserSummaryResponse> listUsers(User.Role role,
                                               String province, String city, String district,
                                               String town, String village, String keyword) {
        boolean hasRegion = hasAny(province, city, district, town, village);

        // 地区范围 → 棚主ID集合（OWNER/WORKER 按此归属判断）；一次性赋值保证 lambda 可用
        final Set<Long> regionOwnerIds;
        if (hasRegion) {
            List<Greenhouse> ghs = regionService.getGreenhousesByRegion(province, city, district, town, village);
            regionOwnerIds = ghs.stream().map(Greenhouse::getOwnerId).collect(Collectors.toSet());
            if (regionOwnerIds.isEmpty()) {
                return Collections.emptyList();
            }
        } else {
            regionOwnerIds = null;
        }

        List<User> users;
        if (role != null) {
            users = userRepository.findByRole(role);
        } else {
            users = userRepository.findAll();
        }

        String kw = keyword == null ? null : keyword.trim().toLowerCase();

        // 先过滤，再批量取地区归属文本（避免逐用户查询大棚）
        List<User> filtered = users.stream()
                .filter(u -> regionOwnerIds == null || inRegion(u, regionOwnerIds))
                .filter(u -> kw == null || kw.isEmpty() || matchesKeyword(u, kw))
                .collect(Collectors.toList());

        Map<Long, String> regionTextByOwner = buildRegionTextMap(filtered);

        return filtered.stream()
                .map(u -> {
                    UserSummaryResponse resp = UserSummaryResponse.fromEntity(u);
                    Long regionOwnerId = regionOwnerKey(u);
                    resp.setRegionText(regionOwnerId != null
                            ? regionTextByOwner.getOrDefault(regionOwnerId, "")
                            : "");
                    return resp;
                })
                .collect(Collectors.toList());
    }

    // ===== 用户列表辅助 =====

    private boolean hasAny(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private boolean inRegion(User u, Set<Long> regionOwnerIds) {
        Long key = regionOwnerKey(u);
        return key != null && regionOwnerIds.contains(key);
    }

    private Long regionOwnerKey(User u) {
        return switch (u.getRole()) {
            case OWNER -> u.getId();
            case WORKER -> u.getOwnerId();
            default -> null;
        };
    }

    private boolean matchesKeyword(User u, String kw) {
        return (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw))
                || (u.getRealName() != null && u.getRealName().toLowerCase().contains(kw))
                || (u.getPhone() != null && u.getPhone().toLowerCase().contains(kw));
    }

    /**
     * 批量计算棚主ID → 地区文本（取该棚主第一个大棚的五级地区拼接）
     */
    private Map<Long, String> buildRegionTextMap(List<User> users) {
        Set<Long> ownerIds = new HashSet<>();
        for (User u : users) {
            Long key = regionOwnerKey(u);
            if (key != null) {
                ownerIds.add(key);
            }
        }
        Map<Long, String> result = new HashMap<>();
        if (ownerIds.isEmpty()) {
            return result;
        }
        List<Greenhouse> all = greenhouseRepository.findByOwnerIdIn(ownerIds);
        Map<Long, List<Greenhouse>> byOwner = all.stream()
                .collect(Collectors.groupingBy(Greenhouse::getOwnerId));
        byOwner.forEach((oid, ghs) -> result.put(oid, firstRegion(ghs)));
        return result;
    }

    private String firstRegion(List<Greenhouse> ghs) {
        for (Greenhouse gh : ghs) {
            List<String> parts = new ArrayList<>();
            if (isNotBlank(gh.getProvince())) parts.add(gh.getProvince());
            if (isNotBlank(gh.getCity())) parts.add(gh.getCity());
            if (isNotBlank(gh.getDistrict())) parts.add(gh.getDistrict());
            if (isNotBlank(gh.getTown())) parts.add(gh.getTown());
            if (isNotBlank(gh.getVillage())) parts.add(gh.getVillage());
            if (!parts.isEmpty()) {
                return String.join(" / ", parts);
            }
        }
        return "";
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * 用户详情
     */
    public UserSummaryResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));
        return UserSummaryResponse.fromEntity(user);
    }

    /**
     * 更新用户（角色/状态/基本信息）
     * <p>
     * 不允许通过此接口修改密码，密码修改走独立流程。
     * </p>
     */
    @Transactional
    public UserSummaryResponse updateUser(Long currentUserId, Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        // 不能把自己降级
        if (userId.equals(currentUserId) && request.getRole() != null && request.getRole() != User.Role.ADMIN) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能修改自己的管理员角色");
        }

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            // 检查手机号是否被其他用户占用
            userRepository.findByPhone(request.getPhone()).ifPresent(other -> {
                if (!other.getId().equals(userId)) {
                    throw new BusinessException(ErrorCode.PHONE_EXISTS);
                }
            });
            user.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userRepository.save(user);
        log.info("管理员更新用户: userId={}, role={}, status={}", userId, request.getRole(), request.getStatus());
        return UserSummaryResponse.fromEntity(user);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long currentUserId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        // ��能删除自己
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能删除自己");
        }

        userRepository.delete(user);
        log.info("管理员删除用户: userId={}, username={}", userId, user.getUsername());
    }

    // ===== 角色统计 =====

    /**
     * 角色列表 + 各角色人数统计
     */
    public List<RoleCountResponse> getRoleStats() {
        return Arrays.stream(User.Role.values())
                .map(role -> {
                    long count = userRepository.countByRole(role);
                    return RoleCountResponse.builder()
                            .role(role.name())
                            .roleLabel(getRoleLabel(role))
                            .count(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ===== 辅助方法 =====

    private String getRoleLabel(User.Role role) {
        return switch (role) {
            case ADMIN -> "管理员";
            case OWNER -> "棚主";
            case WORKER -> "员工";
            case EXPERT -> "专家";
        };
    }
}
