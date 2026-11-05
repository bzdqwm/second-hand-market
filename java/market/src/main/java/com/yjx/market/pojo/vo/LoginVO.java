package com.yjx.market.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功后返回给前端的数据。
 * 注意：绝不包含 password。
 */
@Data
@Builder
public class LoginVO {

    /** JWT 令牌 */
    private String token;
    /** 用户 id */
    private Long id;
    /** 用户名 */
    private String username;
    /** 昵称 */
    private String nickname;
    /** 头像地址 */
    private String avatar;
    /** 角色：USER / ADMIN */
    private String role;
}