package com.yjx.market.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表卡片（首页瀑布流用）。
 * <p>
 * 只放卡片需要的字段——description 可能几千字，列表里绝不能带。
 */
@Data
public class ItemCardVO {

    private Long id;

    private String title;

    private BigDecimal price;

    /** 封面缩略图的签名地址（30 分钟有效，页面刷新会重新签） */
    private String coverThumbUrl;

    /** 卖家昵称 */
    private String sellerNickname;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}