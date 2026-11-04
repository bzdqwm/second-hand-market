package com.yjx.market.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审核记录表 audit_record
 * <p>
 * 机审与人工分开落库；machineResult 与 result 的差集，
 * 就是后续做「规则缺口 / 误杀分析」的数据源。
 */
@Data
@TableName("audit_record")
public class AuditRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long itemId;

    /** 审核类型：MACHINE / MANUAL */
    private String type;

    /** 机审结论：PASS / REJECT */
    private String machineResult;

    /** 最终结论：PASS / REJECT */
    private String result;

    /** 风险等级 */
    private String risk;

    /** 风险分 0~1 */
    private BigDecimal score;

    /** 驳回理由 / 说明 */
    private String reason;

    /** 命中的规则 ID，逗号分隔 */
    private String matchedRuleIds;

    /** 规则库版本 */
    private String ruleVer;

    /** 人工操作人 ID，机审为 null */
    private Long operatorId;

    /** 审核时间 */
    private LocalDateTime createTime;
}