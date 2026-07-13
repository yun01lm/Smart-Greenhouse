package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 作物生长周期数据
 */
public class CropCycleData {

    private long id;

    @SerializedName("greenhouseId")
    private long greenhouseId;

    @SerializedName("cropType")
    private String cropType;

    private String variety;

    @SerializedName("plantingDate")
    private String plantingDate;

    @SerializedName("currentStage")
    private String currentStage;

    @SerializedName("stageSource")
    private String stageSource;

    private String status;

    @SerializedName("daysSincePlanting")
    private int daysSincePlanting;

    public long getId() { return id; }
    public long getGreenhouseId() { return greenhouseId; }
    public String getCropType() { return cropType; }
    public String getVariety() { return variety; }
    public String getPlantingDate() { return plantingDate; }
    public String getCurrentStage() { return currentStage; }
    public String getStageSource() { return stageSource; }
    public String getStatus() { return status; }
    public int getDaysSincePlanting() { return daysSincePlanting; }
}
