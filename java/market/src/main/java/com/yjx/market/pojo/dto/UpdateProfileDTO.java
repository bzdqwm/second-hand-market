package com.yjx.market.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改基本资料入参。
 * <p>
 * 只有昵称和头像两个字段 —— userId 从 token 取，role/status 谁都不许改。
 * 少一个字段，就少一条能被伪造的路径。
 */
@Data
public class UpdateProfileDTO {

    /** 昵称：不能为空、不能纯空白、最长 8 字 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 8, message = "昵称最多 8 个字")
    private String nickname;

    /**
     * 头像 objectKey，形如 items/2026/11/20/xxx.jpg。
     * 可以为 null（表示这次不改头像，只改昵称）。
     * 只要传了，就必须是本平台上传接口产出的 key。
     */
    @Size(max = 200, message = "头像地址过长")
    @Pattern(regexp = "^items/.+\\.jpg$", message = "头像地址不合法，请重新上传")
    private String avatar;
}