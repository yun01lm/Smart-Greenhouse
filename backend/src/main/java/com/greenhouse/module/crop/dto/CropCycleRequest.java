package com.greenhouse.module.crop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 作物生长周期请求 DTO
 */
@Data
public class CropCycleRequest {

    /** 大棚ID */
    @NotNull(message = "大棚ID不能为空")
    private Long greenhouseId;

    /** 作物名称 */
    @NotBlank(message = "作物名称不能为空")
    private String cropType;

    /** 品种 */
    private String variety;

    /** 种植日期 */
    @NotNull(message = "种植日期不能为空")
    private LocalDate plantingDate;

    /** 预计收获日期 */
    private LocalDate expectedHarvestDate;

    /** 备注 */
    private String notes;
}
