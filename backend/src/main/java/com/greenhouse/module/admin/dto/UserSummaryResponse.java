package com.greenhouse.module.admin.dto;

import com.greenhouse.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户摘要响应 DTO（管理端用户列表用，不含密码）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {

    private Long id;
    private String username;
    private String phone;
    private String realName;
    private User.Role role;
    private Boolean status;
    private Long ownerId;
    private String expertSpecialty;
    private User.ExpertStatus expertStatus;
    /** R46 用户常驻五级地区 */
    private String province;
    private String city;
    private String district;
    private String town;
    private String village;
    /** 地区归属文本（省/市/县/乡镇/村），由管理端列表接口填充 */
    private String regionText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserSummaryResponse fromEntity(User u) {
        return UserSummaryResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .phone(u.getPhone())
                .realName(u.getRealName())
                .role(u.getRole())
                .status(u.getStatus())
                .ownerId(u.getOwnerId())
                .expertSpecialty(u.getExpertSpecialty())
                .expertStatus(u.getExpertStatus())
                .province(u.getProvince())
                .city(u.getCity())
                .district(u.getDistrict())
                .town(u.getTown())
                .village(u.getVillage())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
