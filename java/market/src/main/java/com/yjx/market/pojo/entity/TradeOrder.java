package com.yjx.market.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表 trade_order
 * <p>
 * idemKey 唯一约束保证幂等（重复提交不会生成两笔订单）；
 * 防超卖 = item.version 乐观锁 + 商品状态流转。
 */
@Data
@TableName("trade_order")
public class TradeOrder {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，唯一 */
    private String orderNo;

    /** 商品 ID */
    private Long itemId;

    /** 买家 ID */
    private Long buyerId;

    /** 卖家 ID */
    private Long sellerId;

    /** 成交金额 */
    private BigDecimal amount;

    /** 状态：UNPAID / PAID / SHIPPED / COMPLETED / CANCELLED */
    private String status;

    /** 幂等键，前端提交时生成 */
    private String idemKey;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 付款时间 */
    private LocalDateTime payTime;

    /** 完成时间 */
    private LocalDateTime finishTime;
}