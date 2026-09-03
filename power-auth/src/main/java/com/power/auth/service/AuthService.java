package com.power.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.power.auth.domain.SysUser;
import com.power.auth.dto.LoginRequest;
import com.power.auth.dto.LoginResponse;
import com.power.auth.mapper.SysUserMapper;
import com.power.common.constant.ClientPlatform;
import com.power.common.constant.ErrorCode;
import com.power.common.constant.TokenCheckResult;
import com.power.common.constant.TokenDenyReason;
import com.power.common.exception.BizException;
import com.power.common.model.LoginUser;
import com.power.middleware.security.JwtTokenProvider;
import com.power.middleware.security.SecurityProperties;
import com.power.middleware.security.TokenSessionService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 认证服务：登录、刷新、登出、用户启停。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
    private final TokenSessionService tokenSessionService;

    /**
     * 使用 {@link LoginRequest} 登录。
     *
     * @param request 登录请求
     * @return 登录响应（含 Token 与权限）
     */
    public LoginResponse login(LoginRequest request) {
        return login(request.getUsername(), request.getPassword(), request.getPlatform());
    }

    /**
     * 按用户名密码与平台登录，并绑定会话。
     *
     * @param username 用户名
     * @param password 密码
     * @param platform 客户端平台
     * @return 登录响应
     */
    public LoginResponse login(String username, String password, ClientPlatform platform) {
        if (platform == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "platform 不能为空，可选 WEB / MOBILE");
        }
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(ErrorCode.AUTH_LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            tokenSessionService.markUserDisabled(user.getId());
            throw new BizException(ErrorCode.AUTH_USER_DISABLED);
        }
        tokenSessionService.clearUserDisabled(user.getId());
        List<String> perms = sysUserMapper.selectPermsByUserId(user.getId());
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPlatform(platform.name());
        loginUser.setAuthorities(perms);
        return issueTokens(loginUser);
    }

    /**
     * 使用 refreshToken 换发新的 access/refresh 令牌。
     *
     * @param refreshToken 刷新令牌
     * @return 新的登录响应
     */
    public LoginResponse refresh(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            if (!"refresh".equals(claims.get("type"))) {
                throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
            }
            String jti = claims.getId();
            if (tokenSessionService.isRefreshKicked(jti)) {
                throw new BizException(ErrorCode.AUTH_KICKED_BY_OTHER_PLATFORM);
            }
            if (!tokenSessionService.isRefreshValid(jti)) {
                throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
            }
            TokenCheckResult check = tokenSessionService.checkAccess(claims);
            if (check == TokenCheckResult.KICKED_BY_OTHER_PLATFORM) {
                throw new BizException(ErrorCode.AUTH_KICKED_BY_OTHER_PLATFORM);
            }
            if (check == TokenCheckResult.USER_DISABLED) {
                throw new BizException(ErrorCode.AUTH_USER_DISABLED);
            }
            if (check != TokenCheckResult.ALLOWED) {
                throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
            }
            Long userId = Long.valueOf(claims.getSubject());
            ClientPlatform platform = TokenSessionService.readPlatform(claims);
            if (platform == null) {
                throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
            }
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
                tokenSessionService.markUserDisabled(userId);
                throw new BizException(ErrorCode.AUTH_USER_DISABLED);
            }
            tokenSessionService.revokeRefresh(jti);
            tokenSessionService.clearOnline(userId, platform);
            List<String> perms = sysUserMapper.selectPermsByUserId(userId);
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(userId);
            loginUser.setUsername(user.getUsername());
            loginUser.setPlatform(platform.name());
            loginUser.setAuthorities(perms);
            return issueTokens(loginUser);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Refresh token failed", ex);
            throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
        }
    }

    /**
     * 登出：拉黑 access、撤销 refresh，并清理在线会话。
     *
     * @param accessToken  访问令牌（可选）
     * @param refreshToken 刷新令牌（可选）
     */
    public void logout(String accessToken, String refreshToken) {
        String accessJti = null;
        String refreshJti = null;
        ClientPlatform platform = null;
        Long userId = null;

        if (accessToken != null && !accessToken.isBlank()) {
            try {
                Claims accessClaims = jwtTokenProvider.parseClaims(accessToken);
                accessJti = accessClaims.getId();
                platform = TokenSessionService.readPlatform(accessClaims);
                userId = Long.valueOf(accessClaims.getSubject());
            } catch (Exception ex) {
                log.warn("Logout ignore invalid access token: {}", ex.getMessage());
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);
                refreshJti = refreshClaims.getId();
                if (platform == null) {
                    platform = TokenSessionService.readPlatform(refreshClaims);
                }
                if (userId == null) {
                    userId = Long.valueOf(refreshClaims.getSubject());
                }
            } catch (Exception ex) {
                log.warn("Logout ignore invalid refresh token: {}", ex.getMessage());
            }
        }

        if (userId != null && platform != null) {
            tokenSessionService.logoutSession(userId, platform, accessJti, refreshJti);
            return;
        }
        // Fallback when platform/userId unknown: best-effort revoke provided tokens
        if (accessJti != null) {
            tokenSessionService.blacklistAccessByJti(
                    accessJti,
                    TokenDenyReason.LOGOUT,
                    Duration.ofSeconds(securityProperties.getAccessTokenExpireSeconds()));
        }
        if (refreshJti != null) {
            tokenSessionService.revokeRefresh(refreshJti);
        }
    }

    /**
     * 禁用用户并使现有会话立即失效。
     *
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        user.setStatus(0);
        sysUserMapper.updateById(user);
        tokenSessionService.markUserDisabled(userId);
    }

    /**
     * 启用用户。
     *
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        user.setStatus(1);
        sysUserMapper.updateById(user);
        tokenSessionService.clearUserDisabled(userId);
    }

    private LoginResponse issueTokens(LoginUser loginUser) {
        long ver = tokenSessionService.currentUserVersion(loginUser.getUserId());
        String accessToken = jwtTokenProvider.createAccessToken(loginUser, ver);
        String refreshToken = jwtTokenProvider.createRefreshToken(loginUser, ver);
        Claims accessClaims = jwtTokenProvider.parseClaims(accessToken);
        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);
        tokenSessionService.storeRefresh(
                refreshClaims.getId(),
                loginUser.getUserId(),
                Duration.ofSeconds(securityProperties.getRefreshTokenExpireSeconds()));
        ClientPlatform platform = ClientPlatform.from(loginUser.getPlatform());
        tokenSessionService.bindLoginSession(
                loginUser.getUserId(),
                platform,
                accessClaims.getId(),
                refreshClaims.getId());
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(securityProperties.getAccessTokenExpireSeconds())
                .userId(loginUser.getUserId())
                .username(loginUser.getUsername())
                .platform(loginUser.getPlatform())
                .authorities(loginUser.getAuthorities())
                .build();
    }
}
