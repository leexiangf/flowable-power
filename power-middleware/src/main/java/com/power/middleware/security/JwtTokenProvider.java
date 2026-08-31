package com.power.middleware.security;

import com.power.common.model.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecurityProperties securityProperties;

    public JwtTokenProvider(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String createAccessToken(LoginUser user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getAccessTokenExpireSeconds());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim("authorities", user.getAuthorities())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey())
                .compact();
    }

    public String createRefreshToken(LoginUser user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityProperties.getRefreshTokenExpireSeconds());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey())
                .compact();
    }

    public LoginUser parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        LoginUser user = new LoginUser();
        user.setUserId(Long.valueOf(claims.getSubject()));
        user.setUsername(claims.get("username", String.class));
        @SuppressWarnings("unchecked")
        List<String> authorities = claims.get("authorities", List.class);
        if (authorities != null) {
            user.setAuthorities(authorities);
        }
        return user;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey secretKey() {
        byte[] keyBytes = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
