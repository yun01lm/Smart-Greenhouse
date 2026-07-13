package com.greenhouse.module.auth.service;

import com.greenhouse.common.BusinessException;
import com.greenhouse.common.ErrorCode;
import com.greenhouse.entity.User;
import com.greenhouse.module.auth.dto.LoginRequest;
import com.greenhouse.module.auth.dto.LoginResponse;
import com.greenhouse.module.auth.dto.RegisterRequest;
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

    /**
     * 用户注册
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 校验角色（APP端不允许注册ADMIN）
        if (request.getRoleEnum() == User.Role.ADMIN) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不允许注册管理员账号");
        }

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
                .realName(user.getRealName())
                .build();
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

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .realName(user.getRealName())
                .build();
    }
}
