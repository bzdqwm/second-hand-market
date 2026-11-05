package com.yjx.market.service;

import com.yjx.market.pojo.dto.LoginDTO;
import com.yjx.market.pojo.dto.RegisterDTO;
import com.yjx.market.pojo.vo.LoginVO;

public interface UserService {

    /** 注册，返回新用户 id */
    Long register(RegisterDTO dto);

    /** 登录，返回 token + 用户信息 */
    LoginVO login(LoginDTO dto);
}