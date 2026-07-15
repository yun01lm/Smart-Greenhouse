package com.greenhouse.config;

import com.greenhouse.module.mqtt.MqttTopicConstants;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 客户端配置
 * <p>
 * 连接 Mosquitto Broker，创建 MqttClient Bean。
 * 订阅逻辑已迁移至 {@link com.greenhouse.module.mqtt.MqttSubscriber}。
 * Topic 常量统一由 {@link com.greenhouse.module.mqtt.MqttTopicConstants} 管理。
 * </p>
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttConfig {

    private String broker;
    private String username;
    private String password;
    private String clientId;

    @Bean
    public MqttClient mqttClient() throws MqttException {
        MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);

        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
        }
        if (password != null && !password.isEmpty()) {
            options.setPassword(password.toCharArray());
        }

        client.connect(options);
        log.info("MQTT 连接成功: broker={}, clientId={}", broker, clientId);
        return client;
    }
}
