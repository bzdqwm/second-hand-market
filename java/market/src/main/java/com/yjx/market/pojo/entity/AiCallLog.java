package com.yjx.market.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用日志表 ai_call_log
 * <p>
 * 是「采纳率 / 耗时 / 拒答率」这些指标的唯一数据来源，
 * 每次调 Python AI 服务都要落一条。
 */
@Data
@TableName("ai_call_log")
public class AiCallLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联商品 ID，找货场景为 null */
    private Long itemId;

    /** 调用类型：MODERATE / CATEGORY / PRICE / DESCRIBE / QA / GUIDE */
    private String type;

    /** 耗时（毫秒） */
    private Integer costMs;

    /** 消耗 token 数 */
    private Integer tokens;

    /** 问答是否有依据：1 有 / 0 无 */
    private Integer grounded;

    /** 用户是否采纳：1 是 / 0 否 */
    private Integer accepted;

    /** 调用时间 */
    private LocalDateTime createTime;
}