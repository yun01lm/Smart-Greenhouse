package com.greenhouse.module.greenhouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 大棚创建/更新请求 DTO
 */
@Data
public class GreenhouseRequest {

    @NotBlank(message = "大棚名称不能为空")
    @Size(max = 100, message = "大棚名称最长100字")
    private String name;

    @Size(max = 200, message = "位置描述最长200字")
    private String location;

    @Size(max = 50, message = "作物类型最长50字")
    private String cropType;

    // 五级地址
    private String province;
    private String city;
    private String district;
    private String town;
    private String village;

    /** 管理员代建时指定归属棚主（OWNER 创建时忽略） */
    private Long ownerId;
}
