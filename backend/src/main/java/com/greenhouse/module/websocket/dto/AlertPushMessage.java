package com.greenhouse.module.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 预警推送消息
 * <p>
 * 推送到 /topic/greenhouse/{greenhouseId}/alerts
 * 为步骤 10（C6 预警引擎）做准备。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertPushMessage {

    /** 消息类型：ALERT */
    private String type;

    /** 预警ID */
    private Long alertId;

    /** 大棚ID */
    private Long greenhouseId;

    /** 预警级别：INFO / WARNING / CRITICAL */
    private String level;

    /** 预警标题 */
    private String title;

    /** 预警内容 */
    private String content;

    /** 传感器类型 */
    private String sensorType;

    /** 当前读数 */
    private Double currentValue;

    /** 阈值 */
    private Double thresholdValue;

    /** 推送时间 */
    private Instant timestamp;
}
