package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 大棚创建/更新请求（R45）
 * 对应后端 GreenhouseRequest：name 必填，五级地区可选，ownerId 仅管理员代建使用
 */
public class GreenhouseRequest {

    private String name;

    @SerializedName("cropType")
    private String cropType;

    private String location;

    private String province;
    private String city;
    private String district;
    private String town;
    private String village;

    @SerializedName("ownerId")
    private Long ownerId;

    public GreenhouseRequest(String name, String cropType, String location,
                             String province, String city, String district,
                             String town, String village) {
        this.name = name;
        this.cropType = cropType;
        this.location = location;
        this.province = province;
        this.city = city;
        this.district = district;
        this.town = town;
        this.village = village;
    }
}
