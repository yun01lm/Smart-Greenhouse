package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 员工信息模型（棚主端，R26）
 * <p>对应后端 EmployeeResponse：id/username/realName/phone/role/createdAt。</p>
 */
public class EmployeeItem {

    private long id;
    private String username;

    @SerializedName("realName")
    private String realName;

    private String phone;

    /** WORKER（普通员工）/ TECHNICIAN（技术员） */
    private String role;

    /** 员工被授权的大棚名称列表（R26.1） */
    private List<String> greenhouseNames;

    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getRealName() { return realName; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getCreatedAt() { return createdAt; }

    public boolean isTechnician() { return "TECHNICIAN".equals(role); }
    public List<String> getGreenhouseNames() { return greenhouseNames; }
}