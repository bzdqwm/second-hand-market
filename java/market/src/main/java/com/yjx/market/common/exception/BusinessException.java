package com.yjx.market.common.exception;

import com.yjx.market.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：表示“预期之内”的错误（商品不存在、库存不足、权限不够…）。
 * 继承 RuntimeException 是为了免去到处写 throws。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    /** 只给提示，状态码默认 500 BUSINESS_ERROR */
    public BusinessException(String message) {
        super(message);
        this.resultCode = ResultCode.BUSINESS_ERROR;
    }

    /** 指定状态码，比如 NOT_FOUND / FORBIDDEN */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}