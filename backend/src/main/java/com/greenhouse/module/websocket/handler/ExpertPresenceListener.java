package com.greenhouse.module.websocket.handler;

import com.greenhouse.entity.ExpertAvailability;
import com.greenhouse.repository.ExpertAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * 专家在线状态 WebSocket 兜底（R9 新增）
 * <p>
 * 专家通过 Web/App 建立 STOMP 连接时自动置在线，断开时自动置离线。
 * 与 AuthService「登录置在线 / 登出置离线」配合，形成完整闭环：
 * 登录/连接 → 在线；登出/断线 → 离线（含浏览器标签关闭、断网等异常退出场景）。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpertPresenceListener {

    private final ExpertAvailabilityRepository availabilityRepository;

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal instanceof StompAuthInterceptor.StompPrincipal sp
                && "EXPERT".equals(sp.role())) {
            setOnline(sp.userId(), true);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal instanceof StompAuthInterceptor.StompPrincipal sp
                && "EXPERT".equals(sp.role())) {
            setOnline(sp.userId(), false);
        }
    }

    private void setOnline(Long expertId, boolean online) {
        ExpertAvailability av = availabilityRepository.findByExpertId(expertId)
                .orElse(ExpertAvailability.builder().expertId(expertId).maxConcurrent(5).build());
        av.setIsOnline(online ? 1 : 0);
        av.setLastActiveAt(LocalDateTime.now());
        availabilityRepository.save(av);
        log.info("专家在线状态(WS): expertId={}, online={}", expertId, online);
    }
}
