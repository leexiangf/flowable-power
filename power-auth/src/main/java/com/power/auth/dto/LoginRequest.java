package com.power.auth.dto;

import com.power.common.constant.ClientPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 统一登录请求。
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    /** 用户名 */
    @NotBlank
    @Schema(description = "用户名", example = "admin")
    private String username;

    /** 密码 */
    @NotBlank
    @Schema(description = "密码", example = "admin123")
    private String password;

    /** 客户端平台：WEB / MOBILE */
    @NotNull
    @Schema(description = "客户端平台：WEB / MOBILE", example = "WEB")
    private ClientPlatform platform;
}
