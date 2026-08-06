package com.greenhouse.module.auth.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.PasswordPolicy;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.ExpertAvailability;
import com.greenhouse.entity.User;
import com.greenhouse.module.auth.dto.ChangePasswordRequest;
import com.greenhouse.module.auth.dto.LoginRequest;
import com.greenhouse.module.auth.dto.LoginResponse;
import com.greenhouse.module.auth.dto.RegisterRequest;
import com.greenhouse.repository.ExpertAvailabilityRepository;
import com.greenhouse.repository.UserRepository;
import com.greenhouse.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final ExpertAvailabilityRepository availabilityRepository;

    /**
     * 用户注册
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 校验角色（APP端不允许注册ADMIN）
        if (request.getRoleEnum() == User.Role.ADMIN) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不允许注册管理员账号");
        }

        // 校验密码复杂度（>=8位，含字母+数字，规则见 PasswordPolicy）
        PasswordPolicy.validate(request.getPassword());

        // 校验用户名唯一
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 校验手机号唯一
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }

        // 员工必须指定归属棚主
        if (request.getRoleEnum() == User.Role.WORKER && request.getOwnerId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "员工注册必须指定归属棚主");
        }

        // 构建用户实体
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .realName(request.getRealName())
                .role(request.getRoleEnum())
                .ownerId(request.getOwnerId())
                .expertSpecialty(request.getExpertSpecialty())
                .expertStatus(request.getRoleEnum() == User.Role.EXPERT
                        ? User.ExpertStatus.OFFLINE : null)
                .status(true)
                .build();

        user = userRepository.save(user);
        log.info("用户注册成功: username={}, role={}", user.getUsername(), user.getRole());

        // 注册成功直接返回Token（免去二次登录）
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .refreshToken(jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername()))
                .realName(user.getRealName())
                .build();
    }

    /**
     * 用户自助修改密码（R16，全端通用）
     * <p>
     * 校验原密码正确后更新，登录态（JWT）不失效。
     * </p>
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED.getCode(), "原密码不正确");
        }

        PasswordPolicy.validate(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("用户修改密码: userId={}, username={}", userId, user.getUsername());
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // Spring Security 认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 从数据库获取完整用户信息
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        // 检查账号是否被禁用
        if (!user.getStatus()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED.getCode(), "账号已被禁用");
        }

        // 生成JWT
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        log.info("用户登录成功: username={}, role={}", user.getUsername(), user.getRole());

        // 专家登录自动置在线（R9）
        if (user.getRole() == User.Role.EXPERT) {
            setExpertOnline(user.getId(), true);
        }

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .refreshToken(jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername()))
                .realName(user.getRealName())
                .build();
    }

    /**
     * 用户登出（R9）：专家登出自动置离线
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (user.getRole() == User.Role.EXPERT) {
            setExpertOnline(userId, false);
        }
        log.info("用户登出: userId={}, role={}", userId, user.getRole());
    }

    /**
     * 更新专家在线状态（登录置在线 / 登出置离线，R9）
     */
    @Transactional
    protected void setExpertOnline(Long expertId, boolean online) {
        ExpertAvailability availability = availabilityRepository.findByExpertId(expertId)
                .orElse(ExpertAvailability.builder()
                        .expertId(expertId)
                        .maxConcurrent(5)
                        .build());
        availability.setIsOnline(online ? 1 : 0);
        availability.setLastActiveAt(java.time.LocalDateTime.now());
        availabilityRepository.save(availability);
        log.info("专家在线状态自动更新: expertId={}, online={}", expertId, online);
    }

    /**
     * 刷新 Token
     */
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED.getCode(), "Refresh Token 无效或已过期");
        }
        Long userId = jwtTokenProvider.getUserIdFromExpiredToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        String newAccessToken = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());
        return LoginResponse.builder()
                .token(newAccessToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .refreshToken(newRefreshToken)
                .realName(user.getRealName())
                .build();
    }
}