package com.greenhouse.module.permission.dto;

import jakarta.validation.constraints.Size;

/**
 * 更新员工信息请求 DTO
 */
public class UpdateEmployeeRequest {

    @Size(max = 50, message = "姓名最长50字")
    private String realName;

    @Size(max = 20, message = "手机号最长20位")
    private String phone;

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
