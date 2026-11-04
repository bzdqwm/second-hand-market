package com.yjx.market.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商品图片表 item_image
 * <p>
 * 一个商品最多 5 张，sort = 0 的是封面。
 */
@Data
@TableName("item_image")
public class ItemImage {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long itemId;

    /** 原图 OSS Key / URL */
    private String url;

    /** 顺序，0 为封面 */
    private Integer sort;
}