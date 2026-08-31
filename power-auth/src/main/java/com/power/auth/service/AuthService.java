package com.power.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.power.auth.domain.SysUser;
import com.power.auth.dto.LoginRequest;
import com.power.auth.dto.LoginResponse;
import com.power.auth.mapper.SysUserMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.exception.BizException;
import com.power.common.model.LoginUser;
import com.power.middleware.redis.RedisOps;
import com.power.middleware.security.JwtTokenProvider;
import com.power.middleware.security.SecurityProperties;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_KEY_PREFIX = "power:auth:refresh:";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
    private final RedisOps redisOps;

    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.AUTH_LOGIN_FAILED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCode.AUTH_USER_DISABLED);
        }
        List<String> perms = sysUserMapper.selectPermsByUserId(user.getId());
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setAuthorities(perms);
        return issueTokens(loginUser);
    }

    public LoginResponse refresh(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            if (!"refresh".equals(claims.get("type"))) {
                throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
            }
            String jti = claims.getId();
            String cached = redisOps.get(REFRESH_KEY_PREFIX + jti);
            if (cached == null) {
                throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
            }
            Long userId = Long.valueOf(claims.getSubject());
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
                throw new BizException(ErrorCode.AUTH_USER_DISABLED);
            }
            redisOps.delete(REFRESH_KEY_PREFIX + jti);
            List<String> perms = sysUserMapper.selectPermsByUserId(userId);
            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(userId);
            loginUser.setUsername(user.getUsername());
            loginUser.setAuthorities(perms);
            return issueTokens(loginUser);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Refresh token failed", ex);
            throw new BizException(ErrorCode.AUTH_REFRESH_INVALID);
        }
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);
            redisOps.delete(REFRESH_KEY_PREFIX + claims.getId());
        } catch (Exception ex) {
            log.warn("Logout ignore invalid refresh token: {}", ex.getMessage());
        }
    }

    private LoginResponse issueTokens(LoginUser loginUser) {
        String accessToken = jwtTokenProvider.createAccessToken(loginUser);
        String refreshToken = jwtTokenProvider.createRefreshToken(loginUser);
        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);
        redisOps.set(REFRESH_KEY_PREFIX + refreshClaims.getId(), String.valueOf(loginUser.getUserId()),
                Duration.ofSeconds(securityProperties.getRefreshTokenExpireSeconds()));
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(securityProperties.getAccessTokenExpireSeconds())
                .userId(loginUser.getUserId())
                .username(loginUser.getUsername())
                .authorities(loginUser.getAuthorities())
                .build();
    }
}
