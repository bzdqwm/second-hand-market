package com.yjx.market.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图片下载审计表 download_log
 * <p>
 * 记录谁在什么时候下载了哪张图；游客下载 userId 为 null。
 */
@Data
@TableName("download_log")
public class DownloadLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long itemId;

    /** 图片 ID */
    private Long imageId;

    /** 下载人 ID，游客为 null */
    private Long userId;

    /** IP */
    private String ip;

    /** User-Agent */
    private String ua;

    /** 下载时间 */
    private LocalDateTime createTime;
}