package com.yjx.market.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品表 item
 * <p>
 * 注意：
 * 1. categoryId 是用户选的类目，aiCategoryId 是 AI 识别的类目，分两列是为了对比 AI 准确率；
 * 2. version 用于下单乐观锁防超卖（Step 17 再加 @Version 注解和插件）。
 */
@Data
@TableName("item")
public class Item {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卖家 ID */
    private Long sellerId;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 用户选择的类目 */
    private Long categoryId;

    /** AI 识别的类目 */
    private Long aiCategoryId;

    /** 价格，用 BigDecimal 不能用 double（浮点会丢精度） */
    private BigDecimal price;

    /** 状态：DRAFT / PENDING_AUDIT / ON_SALE / OFF_SALE / SOLD / REJECTED */
    private String status;

    /** 机审风险等级：LOW / MEDIUM / HIGH */
    private String aiRisk;

    /** 封面图 URL */
    private String coverUrl;

    /** 乐观锁版本号 */
    private Integer version;

    /** 发布时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}