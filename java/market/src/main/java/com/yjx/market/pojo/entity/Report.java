package com.yjx.market.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报表 report
 */
@Data
@TableName("report")
public class Report {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被举报商品 ID */
    private Long itemId;

    /** 举报人 ID */
    private Long reporterId;

    /** 举报理由 */
    private String reason;

    /** 状态：PENDING / HANDLED / IGNORED */
    private String status;

    /** 处置结果 */
    private String handleResult;

    /** 举报时间 */
    private LocalDateTime createTime;
}