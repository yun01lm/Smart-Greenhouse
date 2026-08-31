package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 大棚信息模型
 */
public class Greenhouse {

    private long id;
    private String name;
    private String location;

    @SerializedName("cropType")
    private String cropType;

    private String province;
    private String city;
    private String district;
    private String town;
    private String village;
    private boolean status;

    public long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getCropType() { return cropType; }
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getDistrict() { return district; }
    public String getTown() { return town; }
    public String getVillage() { return village; }
    public boolean isStatus() { return status; }

    @Override
    public String toString() {
        return name;
    }
}
