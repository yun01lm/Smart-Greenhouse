package com.greenhouse.config;

import com.greenhouse.common.ApiResponse;
import com.greenhouse.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 限流拦截器
 * <p>
 * 基于内存的简单限流方案。每个 IP 每分钟最多允许 REQUEST_LIMIT 次请求，
 * 登录接口单独限制为每分钟 LOGIN_LIMIT 次（防暴力破解）。
 * 生产环境建议替换为 Redis + Bucket4j 方案。
 * </p>
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 通用 API 每分钟最大请求数 */
    private static final int REQUEST_LIMIT = 60;
    /** 登录接口每分钟最大请求数 */
    private static final int LOGIN_LIMIT = 5;
    /** 限流窗口（毫秒） */
    private static final long WINDOW_MS = 60_000;

    /** 存储格式：IP → (窗口起始时间, 计数) */
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String ip = getClientIp(request);
        String path = request.getRequestURI();
        int limit = path.contains("/auth/login") ? LOGIN_LIMIT : REQUEST_LIMIT;

        long now = System.currentTimeMillis();
        WindowCounter counter = counters.computeIfAbsent(ip, k -> new WindowCounter(now));

        synchronized (counter) {
            if (now - counter.windowStart > WINDOW_MS) {
                // 窗口过期，重置
                counter.windowStart = now;
                counter.count = 0;
            }

            counter.count++;

            if (counter.count > limit) {
                log.warn("限流触发: IP={}, path={}, count={}, limit={}", ip, path, counter.count, limit);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                ApiResponse<Void> errorResp = ApiResponse.error(
                        ErrorCode.ACCESS_DENIED.getCode(), "请求过于频繁，请稍后再试");
                response.getWriter().write(objectMapper.writeValueAsString(errorResp));
                return false;
            }
        }

        return true;
    }

    /**
     * 获取客户端真实 IP（考虑反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xrip = request.getHeader("X-Real-IP");
        if (xrip != null && !xrip.isEmpty()) {
            return xrip;
        }
        return request.getRemoteAddr();
    }

    /**
     * 滑动窗口计数器
     */
    private static class WindowCounter {
        long windowStart;
        int count;

        WindowCounter(long start) {
            this.windowStart = start;
            this.count = 0;
        }
    }
}
