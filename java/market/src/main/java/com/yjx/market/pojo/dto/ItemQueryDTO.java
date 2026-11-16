package com.yjx.market.pojo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品列表查询入参（GET 的 query 参数）。
 */
@Data
public class ItemQueryDTO {

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码必须大于 0")
    private Integer page = 1;

    /** 每页条数，上限 50 防止一次拉全表 */
    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 50, message = "每页最多 50 条")
    private Integer size = 12;

    /** 类目 ID（传一级类目时自动包含子类目） */
    private Long categoryId;

    /** 关键词，模糊匹配标题和描述 */
    private String keyword;

    /** 最低价 */
    @DecimalMin(value = "0", message = "最低价不能为负")
    private BigDecimal minPrice;

    /** 最高价 */
    @DecimalMin(value = "0", message = "最高价不能为负")
    private BigDecimal maxPrice;

    /** 排序：latest / price_asc / price_desc */
    private String sort = "latest";
}