package com.yjx.market.controller;

import com.yjx.market.common.result.Result;
import com.yjx.market.common.util.SecurityUtil;
import com.yjx.market.pojo.dto.UpdatePasswordDTO;
import com.yjx.market.pojo.dto.UpdateProfileDTO;
import com.yjx.market.pojo.vo.UserVO;
import com.yjx.market.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户相关接口。全部需要登录（SecurityConfig 的 anyRequest().authenticated() 兜底）。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 查询当前登录用户的资料 */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.getProfile(SecurityUtil.getCurrentUserId()));
    }

    /** 修改昵称 / 头像 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        userService.updateProfile(SecurityUtil.getCurrentUserId(), dto);
        return Result.ok();
    }

    /** 修改密码（成功后前端负责清 token 并跳登录页） */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        userService.updatePassword(SecurityUtil.getCurrentUserId(), dto);
        return Result.ok();
    }
}