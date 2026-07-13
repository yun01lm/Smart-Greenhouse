package com.greenhouse.repository;

import com.greenhouse.entity.WeatherCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 天气缓存 Repository
 */
@Repository
public interface WeatherCacheRepository extends JpaRepository<WeatherCache, Long> {

    /**
     * 查询某位置最新的天气缓存记录
     */
    Optional<WeatherCache> findTopByLocationOrderByUpdatedAtDesc(String location);
}
