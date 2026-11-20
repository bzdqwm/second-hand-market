package com.yjx.market.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核详情：左边看商品，右边看机审结论。
 */
@Data
public class AuditDetailVO {

    private Long id;

    private String title;

    private String description;

    private BigDecimal price;

    private Long categoryId;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<ItemImageVO> images;

    private SellerVO seller;

    /** 最近一次机审结论（没有则为 null） */
    private AuditRecordVO machineRecord;
}