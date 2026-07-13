package com.greenhouse.module.greenhouse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 地区统计响应 DTO（管理员功能）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionStatsResponse {

    /** 地区名称（省/市/区） */
    private String name;

    /** 大棚数量 */
    private Long greenhouseCount;

    /** 棚主数量 */
    private Long ownerCount;

    /** 上级地区（如城市对应的省份） */
    private String parent;
}
