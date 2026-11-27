package com.yjx.market.pojo.vo;

import lombok.Data;

/** 一个候选类目。 */
@Data
public class AiCategoryHitVO {
    private Long categoryId;
    private String name;
    private String path;
    private Double score;
}