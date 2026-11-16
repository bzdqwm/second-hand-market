package com.yjx.market.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 上下架入参。
 */
@Data
public class ItemStatusDTO {

    /** 目标状态：ON_SALE / OFF_SALE */
    @NotBlank(message = "请指定目标状态")
    private String status;
}