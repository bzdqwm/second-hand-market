package com.yjx.market.common.exception;

import com.yjx.market.common.result.Result;
import com.yjx.market.common.result.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：把各种异常统一翻译成 Result。
 * 原则：堆栈进日志，前端只拿 code + msg，绝不泄露内部细节。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** ① 业务异常：我们自己 throw 的，属于预期内 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常 | code={} | msg={}", e.getResultCode().getCode(), e.getMessage());
        return Result.fail(e.getResultCode(), e.getMessage());
    }

    /** ② 参数校验失败：@Valid @RequestBody 触发 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ResultCode.BAD_REQUEST.getMsg());
        log.warn("参数校验失败 | {}", msg);
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** ③ 参数校验失败：@Validated + @RequestParam 触发 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(ResultCode.BAD_REQUEST.getMsg());
        log.warn("参数校验失败 | {}", msg);
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** ④ 少传了必填参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String msg = "缺少必填参数：" + e.getParameterName();
        log.warn(msg);
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** ⑤ 请求体 JSON 格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败 | {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误，请检查 JSON");
    }

    /** ⑥ 路径不存在 → 404，别让它被兜底成 500 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        log.warn("路径不存在 | {}", e.getResourcePath());
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在：" + e.getResourcePath());
    }

    /** ⑦ 兜底：所有没预料到的异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未捕获异常", e);          // 完整堆栈进日志
        return Result.fail(ResultCode.SYSTEM_ERROR);   // 前端只拿到一句友好提示
    }
}