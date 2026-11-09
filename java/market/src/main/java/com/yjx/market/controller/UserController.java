package com.yjx.market.controller;

import com.yjx.market.common.result.Result;
import com.yjx.market.common.util.SecurityUtil;
import com.yjx.market.pojo.vo.UserVO;
import com.yjx.market.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关接口。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 查询当前登录用户的资料 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        Long userId = SecurityUtil.getCurrentUserId();
        return Result.ok(userService.getProfile(userId));
    }
}