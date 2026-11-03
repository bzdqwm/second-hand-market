package com.yjx.market.common.result;



/**
 * 统一状态码。
 * 只放通用的，具体业务的码后续各自模块自己定（如 ITEM_NOT_FOUND）。
 */
public enum ResultCode {

    SUCCESS(200, "成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "请求的资源不存在"),

    BUSINESS_ERROR(500, "业务处理失败"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
