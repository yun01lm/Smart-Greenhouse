package com.greenhouse.module.websocket.handler;

import com.greenhouse.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * STOMP 连接认证拦截器
 * <p>
 * 在 STOMP CONNECT 阶段从 Header 中提取 JWT Token 并校验。
 * 校验通过后将用户身份信息存入 STOMP Session，后续消息可用。
 * </p>
 *
 * <h3>客户端连接方式</h3>
 * <pre>
 * const client = Stomp.client('ws://host:8080/ws/connect');
 * client.connect({
 *     Authorization: 'Bearer eyJhbGciOiJIUzI1NiJ9...'
 * }, onConnected, onError);
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从 CONNECT 帧的 Header 中提取 Authorization
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("STOMP CONNECT 缺少 Authorization Header");
                throw new IllegalArgumentException("缺少认证信息");
            }

            String token = authHeader.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("STOMP CONNECT Token 无效");
                throw new IllegalArgumentException("Token 无效或已过期");
            }

            Long userId = jwtTokenProvider.getUserId(token);
            String username = jwtTokenProvider.getUsername(token);
            String role = jwtTokenProvider.getRole(token);

            // 将用户信息存入 STOMP Session
            StompPrincipal principal = new StompPrincipal(userId, username, role);
            accessor.setUser(principal);

            log.debug("STOMP 认证成功: userId={}, username={}, role={}", userId, username, role);
        }

        return message;
    }

    /**
     * STOMP 连接的用户主体
     */
    public record StompPrincipal(Long userId, String username, String role) implements Principal {
        @Override
        public String getName() {
            return username;
        }
    }
}
