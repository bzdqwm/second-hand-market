package com.yjx.market.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 详情页里的卖家信息。
 * <p>
 * 只暴露展示需要的字段——username 不对外（避免被拿去撞库），
 * password / role / status 更不可能出现。
 */
@Data
public class SellerVO {

    private Long id;

    private String nickname;

    private String avatar;

    /** 注册时间，展示成「2026-11-01 入驻」 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}