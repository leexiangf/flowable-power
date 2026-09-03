package com.power.auth.controller;

import com.power.auth.dto.LoginRequest;
import com.power.auth.dto.LoginResponse;
import com.power.auth.dto.LogoutRequest;
import com.power.auth.dto.RefreshRequest;
import com.power.auth.service.AuthService;
import com.power.common.constant.ClientPlatform;
import com.power.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、刷新、登出。
 */
@Tag(name = "认证", description = "登录、刷新令牌、登出")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 统一登录（请求体需包含 platform）。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Operation(summary = "统一登录", description = "请求体需包含 platform：WEB / MOBILE。返回 accessToken 与 refreshToken。", security = {})
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    /**
     * Web 端登录（平台固定为 WEB）。
     *
     * @param body 用户名密码
     * @return 登录响应
     */
    @Operation(summary = "Web 端登录", description = "固定平台为 WEB，无需传 platform 字段。", security = {})
    @PostMapping("/login/web")
    public R<LoginResponse> loginWeb(@Valid @RequestBody PlatformLoginBody body) {
        return R.ok(authService.login(body.getUsername(), body.getPassword(), ClientPlatform.WEB));
    }

    /**
     * 移动端登录（平台固定为 MOBILE）。
     *
     * @param body 用户名密码
     * @return 登录响应
     */
    @Operation(summary = "移动端登录", description = "固定平台为 MOBILE，无需传 platform 字段。", security = {})
    @PostMapping("/login/mobile")
    public R<LoginResponse> loginMobile(@Valid @RequestBody PlatformLoginBody body) {
        return R.ok(authService.login(body.getUsername(), body.getPassword(), ClientPlatform.MOBILE));
    }

    /**
     * 刷新访问令牌。
     *
     * @param request 刷新请求
     * @return 新的登录响应
     */
    @Operation(summary = "刷新令牌", description = "使用 refreshToken 换取新的 accessToken / refreshToken。被顶号时返回 20004。", security = {})
    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return R.ok(authService.refresh(request.getRefreshToken()));
    }

    /**
     * 登出并失效会话。
     *
     * @param request 登出请求（可选）
     * @return 空成功响应
     */
    @Operation(summary = "登出", description = "将 accessToken 加入黑名单，并清理对应会话；可只传其一。", security = {})
    @PostMapping("/logout")
    public R<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        if (request != null) {
            authService.logout(request.getAccessToken(), request.getRefreshToken());
        }
        return R.ok();
    }

    /**
     * 分端登录请求体（无 platform 字段）。
     */
    @Data
    public static class PlatformLoginBody {

        /** 用户名 */
        @NotBlank
        private String username;

        /** 密码 */
        @NotBlank
        private String password;
    }
}
