package com.yjx.market.pojo.vo;

import lombok.Data;

/**
 * 详情页的一张图。
 */
@Data
public class ItemImageVO {

    /** 原图签名地址（带水印） */
    private String url;

    /** 缩略图签名地址（不带水印） */
    private String thumbUrl;
}