package com.yjx.market.common.controller;

import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.result.Result;
import com.yjx.market.common.result.ResultCode;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

/**
 * 临时：专门用来验证全局异常处理，Step 3 验收完可以整个删掉。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/test")
public class ErrorTestController {

    /** ① 业务异常 */
    @GetMapping("/biz")
    public Result<Void> biz() {
        throw new BusinessException(ResultCode.BUSINESS_ERROR, "商品已下架");
    }

    /** ② 未预料的异常 */
    @GetMapping("/npe")
    public Result<Void> npe() {
        String s = null;
        s.length();          // 故意的空指针
        return Result.ok();
    }

    /** ③ 参数校验 */
    @GetMapping("/param")
    public Result<Integer> param(
            @RequestParam @Min(value = 1, message = "年龄必须大于 0") Integer age) {
        return Result.ok(age);
    }
    /** ④ traceId 验证：同一个请求打三条日志 */
    @GetMapping("/trace")
    public Result<Void> trace() {
        log.info("第一条日志");
        log.info("第二条日志");
        log.info("第三条日志");
        return Result.ok();
    }
}