package com.yjx.market.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 待审列表的一行。
 */
@Data
public class AuditItemVO {

    private Long id;

    private String title;

    private BigDecimal price;

    /** 缩略图签名地址 */
    private String coverThumbUrl;

    private String sellerNickname;

    /** 机审风险等级（没跑过机审时为 null） */
    private String risk;

    private BigDecimal score;

    /** 机审给的说明，一眼看出「为什么进队列」 */
    private String machineReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}