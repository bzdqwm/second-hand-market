package com.yjx.market.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 「我发布的」列表行。
 * <p>
 * 和 ItemCardVO 的区别：这个带 status 和 rejectReason，
 * 而且不过滤状态（自己的商品，好坏都要看得见）。
 */
@Data
public class MyItemVO {

    private Long id;

    private String title;

    private BigDecimal price;

    private String coverThumbUrl;

    /** DRAFT / PENDING_AUDIT / ON_SALE / OFF_SALE / SOLD / REJECTED */
    private String status;

    /** 被驳回时，最近一次人工审核给的理由；其它状态为 null */
    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}