package com.yjx.market.controller;

import com.yjx.market.common.result.Result;
import com.yjx.market.pojo.dto.LoginDTO;
import com.yjx.market.pojo.dto.RegisterDTO;
import com.yjx.market.pojo.vo.LoginVO;
import com.yjx.market.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关接口：注册、登录。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 注册 */
    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Valid RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }
}