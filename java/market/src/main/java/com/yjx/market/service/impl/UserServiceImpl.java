package com.yjx.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yjx.market.common.exception.BusinessException;
import com.yjx.market.common.oss.OssTemplate;
import com.yjx.market.common.result.ResultCode;
import com.yjx.market.common.util.JwtUtil;
import com.yjx.market.mapper.UserMapper;
import com.yjx.market.pojo.dto.LoginDTO;
import com.yjx.market.pojo.dto.RegisterDTO;
import com.yjx.market.pojo.dto.UpdatePasswordDTO;
import com.yjx.market.pojo.dto.UpdateProfileDTO;
import com.yjx.market.pojo.entity.User;
import com.yjx.market.pojo.vo.LoginVO;
import com.yjx.market.pojo.vo.UserVO;
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
    private final OssTemplate ossTemplate;

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
                // 前端会把它存进 localStorage 给顶栏头像用，必须是签名地址而不是 objectKey
                .avatar(ossTemplate.avatarUrl(user.getAvatar()))
                .role(user.getRole())
                .build();
    }

    @Override
    public UserVO getProfile(Long userId) {
        // ① 按主键查（走索引，很快）
        User user = userMapper.selectById(userId);

        // ② token 有效但用户已从库里消失（被删号）→ 视同登录失效
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在或已注销");
        }

        // ③ 实体 → VO，只搬该给的字段
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        // 库里存的是 objectKey，私有桶必须现场签名，前端才能直接当 img src 用
        vo.setAvatar(ossTemplate.avatarUrl(user.getAvatar()));
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        // ① 确认用户还在（token 有效但号被删了 → 按登录失效处理）
        getExistingUser(userId);

        String nickname = dto.getNickname().trim();

        // ② 只带「要改的字段」更新，不把整个实体丢进去
        User update = new User();
        update.setId(userId);
        update.setNickname(nickname);

        // ③ avatar 为 null = 这次不改头像，null 不会进 SQL，头像保持原样
        if (dto.getAvatar() != null) {
            update.setAvatar(dto.getAvatar().trim());
        }

        userMapper.updateById(update);

        log.info("资料已更新：userId={}, nickname={}, 头像{}变更",
                userId, nickname, dto.getAvatar() == null ? "未" : "已");
    }

    @Override
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        User user = getExistingUser(userId);

        // ① 原密码必须对：这是「手机被别人拿到」时的最后一道门
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "原密码不正确");
        }

        // ② 新旧不能相同：用 matches 比，不是 equals 比字符串
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能与原密码相同");
        }

        // ③ BCrypt 重新编码后写回
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);

        log.info("密码已修改：userId={}", userId);
    }

    /** 取当前用户；取不到就按登录失效处理 */
    private User getExistingUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在或已注销");
        }
        return user;
    }
}