package com.yjx.market.controller;

import com.yjx.market.common.result.Result;
import com.yjx.market.common.util.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /** 临时验证用：能进来就说明 ADMIN 权限生效了 */
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("admin ok, userId=" + SecurityUtil.getCurrentUserId());
    }
}