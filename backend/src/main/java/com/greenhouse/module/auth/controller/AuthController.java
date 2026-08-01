package com.greenhouse.module.auth.controller;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.entity.User;
import com.greenhouse.module.auth.dto.LoginRequest;
import com.greenhouse.module.auth.dto.LoginResponse;
import com.greenhouse.module.auth.dto.RegisterRequest;
import com.greenhouse.module.auth.dto.UserProfileResponse;
import com.greenhouse.module.auth.service.AuthService;
import com.greenhouse.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证 API
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * 用户注册
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ApiResponse.success("注册成功", response);
    }

    /**
     * 用户登录
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.success("登录成功", response);
    }

    /**
     * 获取当前用户信息
     * GET /api/v1/auth/profile
     */
    /**
     * 刷新 Token
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.error("refreshToken 不能为空");
        }
        LoginResponse response = authService.refreshToken(refreshToken);
        return ApiResponse.success("Token 刷新成功", response);
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> profile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.greenhouse.common.BusinessException(
                        com.greenhouse.common.ErrorCode.RESOURCE_NOT_FOUND));

        return ApiResponse.success(UserProfileResponse.fromEntity(user));
    }
}
