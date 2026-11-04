package com.yjx.market.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 sys_user
 * <p>
 * 买卖同账号，用 role 区分 USER / ADMIN。
 */
@Data
@TableName("sys_user")
public class User {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，唯一 */
    private String username;

    /** BCrypt 加密后的密码，绝不存明文，也不返回给前端 */
    @JsonIgnore
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 角色：USER / ADMIN */
    private String role;

    /** 状态：1 正常 / 0 封禁 */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}