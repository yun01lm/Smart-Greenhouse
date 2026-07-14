package com.greenhouse.module.admin.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.User;
import com.greenhouse.module.admin.dto.RoleCountResponse;
import com.greenhouse.module.admin.dto.UpdateUserRequest;
import com.greenhouse.module.admin.dto.UserSummaryResponse;
import com.greenhouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
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

    // ===== 用户管理 =====

    /**
     * 用户列表（支持按角色筛选）
     */
    public List<UserSummaryResponse> listUsers(User.Role role) {
        List<User> users;
        if (role != null) {
            users = userRepository.findByRole(role);
        } else {
            users = userRepository.findAll();
        }
        return users.stream()
                .map(UserSummaryResponse::fromEntity)
                .collect(Collectors.toList());
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
    public UserSummaryResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        // 不能把自己降级
        if (user.getId().equals(userId) && request.getRole() != null && request.getRole() != User.Role.ADMIN) {
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
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        // 不能删除自己
        if (user.getId().equals(userId)) {
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
