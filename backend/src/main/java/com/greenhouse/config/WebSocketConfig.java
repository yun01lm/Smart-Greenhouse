package com.greenhouse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 配置
 * <p>
 * 用于实时数据推送和专家聊天。步骤3只做基础配置，步骤9（C11）和后续聊天模块会完善。
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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
        // WebSocket 连接端点
        registry.addEndpoint("/ws/connect")
                .setAllowedOriginPatterns("*");
                // 生产环境需改为 .setAllowedOrigins("具体域名")
    }
}
