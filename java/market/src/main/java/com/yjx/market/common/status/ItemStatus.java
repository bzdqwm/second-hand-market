package com.yjx.market.common.status;

import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.result.ResultCode;

/**
 * 商品状态。数据库里存的是 code 字符串，这里把它变成类型。
 */
public enum ItemStatus {

    DRAFT("DRAFT", "草稿"),
    PENDING_AUDIT("PENDING_AUDIT", "待审核"),
    ON_SALE("ON_SALE", "在售"),
    OFF_SALE("OFF_SALE", "已下架"),
    SOLD("SOLD", "已售出"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String label;

    ItemStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    /** 中文名，只用于报错和日志，不要回传给前端当状态值 */
    public String getLabel() {
        return label;
    }

    /** 数据库字符串 → 枚举；不认识的值直接报错，别猜 */
    public static ItemStatus of(String code) {
        for (ItemStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new BusinessException(ResultCode.BAD_REQUEST, "未知的商品状态：" + code);
    }
}