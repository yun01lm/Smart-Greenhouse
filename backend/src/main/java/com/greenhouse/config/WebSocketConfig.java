package com.greenhouse.config;

import com.greenhouse.module.websocket.handler.StompAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 配置
 * <p>
 * 用于实时数据推送和专家聊天。
 * 步骤 9（C11）完善：添加 STOMP 认证拦截器。
 * </p>
 *
 * <h3>端点说明</h3>
 * <ul>
 *   <li>连接端点：/ws/connect</li>
 *   <li>客户端订阅前缀（服务端→客户端）：/topic, /queue</li>
 *   <li>客户端发送前缀（客户端→服务端）：/app</li>
 *   <li>用户私有消息前缀：/user</li>
 * </ul>
 *
 * <h3>推送主题</h3>
 * <ul>
 *   <li>/topic/greenhouse/{id}/realtime — 实时传感器数据</li>
 *   <li>/topic/greenhouse/{id}/alerts — 预警推送</li>
 *   <li>/topic/device/{id}/status — 设备状态变更</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端订阅前缀（服务端 → 客户端）
        registry.enableSimpleBroker("/topic", "/queue");
        // 客户端发送前缀（客户端 → 服务端）
        registry.setApplicationDestinationPrefixes("/app");
        // 用户私有消息前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 连接端点，允许跨域
        registry.addEndpoint("/ws/connect")
                .setAllowedOriginPatterns("*");
        // 生产环境需改为 .setAllowedOrigins("具体域名")
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 注册 STOMP 认证拦截器
        registration.interceptors(stompAuthInterceptor);
    }
}
