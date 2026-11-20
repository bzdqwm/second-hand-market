package com.yjx.market.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审核记录出参（审核台右侧展示用）。
 */
@Data
public class AuditRecordVO {

    private Long id;

    /** MACHINE / MANUAL */
    private String type;

    /** 机审结论 PASS / REJECT（人工记录为 null） */
    private String machineResult;

    /** 最终结论 PASS / REJECT（机审记录为 null） */
    private String result;

    /** LOW / MEDIUM / HIGH */
    private String risk;

    private BigDecimal score;

    private String reason;

    /** 人工操作人（机审为 null） */
    private Long operatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}