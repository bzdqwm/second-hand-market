package com.yjx.market.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回体。
 * 约定：HTTP 状态码恒为 200，业务成败看 code。
 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功，带数据 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /** 成功，无数据 */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /** 失败，用预定义状态码 */
    public static <T> Result<T> fail(ResultCode rc) {
        return new Result<>(rc.getCode(), rc.getMsg(), null);
    }

    /** 失败，自定义消息（业务异常用） */
    public static <T> Result<T> fail(ResultCode rc, String msg) {
        return new Result<>(rc.getCode(), msg, null);
    }
}