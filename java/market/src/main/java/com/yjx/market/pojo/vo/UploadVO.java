package com.yjx.market.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 图片上传结果。
 */
@Data
@Builder
public class UploadVO {

    /** 对象路径，发布商品时存进 item_image.url */
    private String objectKey;

    /** 原图签名地址（带水印，30 分钟有效） */
    private String url;

    /** 缩略图签名地址（列表页用） */
    private String thumbUrl;

    /** 图片宽度 */
    private Integer width;

    /** 图片高度 */
    private Integer height;
}