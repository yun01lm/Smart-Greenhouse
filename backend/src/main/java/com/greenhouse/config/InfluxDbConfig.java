package com.greenhouse.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB 2.x 客户端配置
 * <p>
 * 连接 InfluxDB 2.7，用于存储和查询时序传感器数据。
 * measurement: sensor_data
 * tags: greenhouse_id, device_id, sensor_type
 * </p>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "influxdb")
public class InfluxDbConfig {

    private String url;
    private String token;
    private String org;
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }

    @Bean
    public WriteApiBlocking writeApiBlocking(InfluxDBClient client) {
        return client.getWriteApiBlocking();
    }

    @Bean
    public QueryApi queryApi(InfluxDBClient client) {
        return client.getQueryApi();
    }
}
