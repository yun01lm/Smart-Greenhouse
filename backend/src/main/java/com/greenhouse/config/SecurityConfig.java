package com.greenhouse.config;

import com.greenhouse.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置
 * <p>
 * 使用无状态 JWT 认证，关闭 CSRF 和 Session。
 * 白名单路径不需要认证，其余路径需要 JWT Token。
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（API 服务不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 无状态会话（JWT 不需要 Session）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 路径权限配置
                .authorizeHttpRequests(auth -> auth
                        // === 白名单（无需认证） ===
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        // WebSocket 握手
                        .requestMatchers("/ws/**").permitAll()
                        // 健康检查
                        .requestMatchers("/actuator/health").permitAll()
                        // Swagger / API文档（如果启用）
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Admin 管理接口（仅 ADMIN 角色）
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // 知识库管理接口（仅 ADMIN 角色）
                        .requestMatchers("/api/v1/knowledge/**").hasRole("ADMIN")
                        // OPTIONS 预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // === 其余全部需要认证 ===
                        .anyRequest().authenticated()
                )
                // JWT 过滤器（在 UsernamePasswordAuthenticationFilter 之前执行）
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager（供登录接口使用）
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
