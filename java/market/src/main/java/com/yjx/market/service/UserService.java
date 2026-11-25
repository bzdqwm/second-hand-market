package com.yjx.market.service;

import com.yjx.market.pojo.dto.LoginDTO;
import com.yjx.market.pojo.dto.RegisterDTO;
import com.yjx.market.pojo.dto.UpdatePasswordDTO;
import com.yjx.market.pojo.dto.UpdateProfileDTO;
import com.yjx.market.pojo.vo.LoginVO;
import com.yjx.market.pojo.vo.UserVO;

public interface UserService {

    /** 注册，返回新用户 id */
    Long register(RegisterDTO dto);

    /** 登录，返回 token + 用户信息 */
    LoginVO login(LoginDTO dto);

    UserVO getProfile(Long userId);

    /**
     * 修改基本资料（昵称 + 头像）。
     *
     * @param userId 从 token 取，不接受前端传
     * @param dto    昵称必填，头像可空（空表示这次不换头像）
     */
    void updateProfile(Long userId, UpdateProfileDTO dto);

    /**
     * 修改密码。校验顺序：原密码对不对 → 新旧是否相同 → BCrypt 重新编码。
     *
     * @param userId 从 token 取
     * @param dto    原密码 + 新密码
     */
    void updatePassword(Long userId, UpdatePasswordDTO dto);
}