package com.yjx.market.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码入参。
 * <p>
 * 只要旧密码 + 新密码两个字段；
 * 新密码的「两次输入一致」由前端校验，后端不重复收一遍 ——
 * 多收一个 confirmPassword 只会多一个可能跟 newPassword 不一致的字段。
 */
@Data
public class UpdatePasswordDTO {

    @NotBlank(message = "请输入原密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 20, message = "新密码长度需在 6~20 位之间")
    private String newPassword;
}