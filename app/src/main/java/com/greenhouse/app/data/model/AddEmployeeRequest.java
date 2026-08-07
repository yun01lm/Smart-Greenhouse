package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 新增员工请求（棚主端，R26 双模式）
 * <p>创建模式：username/realName/phone/password/roleType/greenhouseId；
 * 邀请模式：identifier（已存在账号的用户名或手机号）+ greenhouseId。</p>
 */
public class AddEmployeeRequest {

    /** 邀请模式：用户名或手机号 */
    private String identifier;

    /** 创建模式：用户名 */
    private String username;

    /** 创建模式：真实姓名 */
    @SerializedName("realName")
    private String realName;

    /** 创建模式：手机号 */
    private String phone;

    /** 创建模式：初始密码（>=8位，含字母和数字） */
    private String password;

    /** 员工类型：WORKER / TECHNICIAN，默认 WORKER */
    private String roleType = "WORKER";

    /** 授权大棚ID */
    private Long greenhouseId;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public Long getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(Long greenhouseId) { this.greenhouseId = greenhouseId; }
}