package com.power.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登出请求。
 * <p>
 * 建议同时传入 accessToken 与 refreshToken，以便立即拉黑并清理会话。
 */
@Data
@Schema(description = "登出请求")
public class LogoutRequest {

    /** 访问令牌（写入黑名单，可选但建议传） */
    @Schema(description = "访问令牌")
    private String accessToken;

    /** 刷新令牌（撤销会话） */
    @Schema(description = "刷新令牌")
    private String refreshToken;
}
