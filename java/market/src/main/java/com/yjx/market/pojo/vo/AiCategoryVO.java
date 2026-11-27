package com.yjx.market.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 类目识别结果。
 *
 * 字段名必须和 Python 返回的 JSON 一致（categoryId / candidates / threshold…），
 * 否则反序列化会拿到 null。这也是为什么两边都叫「驼峰」。
 */
@Data
public class AiCategoryVO {

    private String title;

    /** 最像的那一个 */
    private AiCategoryHitVO hit;

    /** Top N 候选（含 hit） */
    private List<AiCategoryHitVO> candidates;

    /** 置信度是否达标：true 才自动填 */
    private Boolean matched;

    /** 当前阈值，方便前端展示「需要 ≥ 0.45」 */
    private Double threshold;
}