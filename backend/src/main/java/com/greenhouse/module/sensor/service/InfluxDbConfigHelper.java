package com.greenhouse.module.sensor.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * InfluxDB 配置辅助类
 * <p>
 * 提供 org/bucket 值给 Flux 查询使用。
 * 之所以不直接注入 InfluxDbConfig，是为了避免循环依赖问题。
 * </p>
 */
@Getter
@Component
public class InfluxDbConfigHelper {

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;
}
