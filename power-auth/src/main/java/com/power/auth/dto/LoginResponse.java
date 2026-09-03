package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录成功响应。
 */
@Data
@Builder
@Schema(description = "登录成功响应")
public class LoginResponse {

    /** 访问令牌（JWT） */
    @Schema(description = "访问令牌")
    private String accessToken;

    /** 刷新令牌 */
    @Schema(description = "刷新令牌")
    private String refreshToken;

    /** 令牌类型，固定 Bearer */
    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType;

    /** accessToken 有效秒数 */
    @Schema(description = "accessToken 有效秒数")
    private Long expiresIn;

    /** 用户 ID */
    @Schema(description = "用户 ID")
    private Long userId;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** 登录平台：WEB / MOBILE */
    @Schema(description = "登录平台")
    private String platform;

    /** 权限码列表 */
    @Schema(description = "权限码列表")
    private List<String> authorities;
}
