package com.greenhouse.module.greenhouse.dto;

import com.greenhouse.entity.Greenhouse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 大棚信息响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GreenhouseResponse {

    private Long id;
    private String name;
    private String location;
    private String cropType;
    private Long ownerId;
    private String province;
    private String city;
    private String district;
    private String town;
    private String village;
    private Boolean status;
    private LocalDateTime createdAt;

    public static GreenhouseResponse fromEntity(Greenhouse g) {
        return GreenhouseResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .location(g.getLocation())
                .cropType(g.getCropType())
                .ownerId(g.getOwnerId())
                .province(g.getProvince())
                .city(g.getCity())
                .district(g.getDistrict())
                .town(g.getTown())
                .village(g.getVillage())
                .status(g.getStatus())
                .createdAt(g.getCreatedAt())
                .build();
    }
}
