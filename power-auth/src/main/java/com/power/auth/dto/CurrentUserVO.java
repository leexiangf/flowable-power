package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户资料。
 */
@Data
@Schema(description = "当前用户信息")
public class CurrentUserVO {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "登录名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "状态：0停用 1正常")
    private Integer status;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "权限码列表")
    private List<String> authorities;
}
