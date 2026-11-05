package com.yjx.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.result.ResultCode;
import com.yjx.market.common.util.JwtUtil;
import com.yjx.market.mapper.UserMapper;
import com.yjx.market.pojo.dto.LoginDTO;
import com.yjx.market.pojo.dto.RegisterDTO;
import com.yjx.market.pojo.entity.User;
import com.yjx.market.pojo.vo.LoginVO;
import com.yjx.market.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public Long register(RegisterDTO dto) {
        // ① 查重：用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已被注册");
        }

        // ② 组装实体：密码加密，角色固定 USER，状态正常
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? dto.getUsername()
                : dto.getNickname());
        user.setRole("USER");
        user.setStatus(1);

        // ③ 插入
        userMapper.insert(user);

        log.info("新用户注册成功：id={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // ① 按用户名查用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);

        // ② 用户不存在 或 密码不对 → 统一提示，防止探测账号
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // ③ 状态检查
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // ④ 签发 token
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        log.info("用户登录成功：id={}, username={}", user.getId(), user.getUsername());

        // ⑤ 组装返回
        return LoginVO.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }
}