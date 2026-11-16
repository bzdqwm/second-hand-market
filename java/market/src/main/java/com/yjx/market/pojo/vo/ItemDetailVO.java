package com.yjx.market.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情出参。
 */
@Data
public class ItemDetailVO {

    private Long id;

    private String title;

    private String description;

    private BigDecimal price;

    private Long categoryId;

    /** 类目全路径，如「数码 / 平板」 */
    private String categoryName;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 图片列表，顺序即 sort 顺序（0 是封面） */
    private List<ItemImageVO> images;

    /** 卖家信息（裁剪过的） */
    private SellerVO seller;

    /** 当前查看者是不是卖家本人 */
    private Boolean owner;
}