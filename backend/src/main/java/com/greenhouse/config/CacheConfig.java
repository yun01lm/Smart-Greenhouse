package com.greenhouse.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * <p>
 * 使用 Caffeine 本地缓存，减少对 InfluxDB 的重复查询压力。
 * 传感器实时数据缓存 5 秒——5 秒内多人刷新仪表盘都命中缓存。
 * </p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)   // 写入后 5 秒过期
                .maximumSize(100)                         // 最多缓存 100 个 key
                .recordStats());                          // 开启统计（便于监控）
        return manager;
    }
}
