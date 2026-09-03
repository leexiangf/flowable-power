package com.power.middleware.security;

import com.power.common.constant.AuthCacheKeys;
import com.power.common.constant.ClientPlatform;
import com.power.common.constant.TokenCheckResult;
import com.power.common.constant.TokenDenyReason;
import com.power.middleware.redis.RedisOps;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Redis-backed session: blacklist / kick reason / online per platform / user version.
 * Gateway checks the same keys (no DB).
 */
@Service
@RequiredArgsConstructor
public class TokenSessionService {

    private final RedisOps redisOps;
    private final SecurityProperties securityProperties;

    public long currentUserVersion(Long userId) {
        String raw = redisOps.get(AuthCacheKeys.userTokenVersion(userId));
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public long bumpUserVersion(Long userId) {
        Long ver = redisOps.increment(AuthCacheKeys.userTokenVersion(userId));
        return ver == null ? 1L : ver;
    }

    public void markUserDisabled(Long userId) {
        redisOps.set(AuthCacheKeys.userDisabled(userId), "1");
        bumpUserVersion(userId);
        clearAllOnline(userId);
    }

    public void clearUserDisabled(Long userId) {
        redisOps.delete(AuthCacheKeys.userDisabled(userId));
    }

    public boolean isUserDisabled(Long userId) {
        return redisOps.hasKey(AuthCacheKeys.userDisabled(userId));
    }

    public void blacklistAccess(Claims accessClaims, String reason) {
        if (accessClaims == null || accessClaims.getId() == null) {
            return;
        }
        Duration ttl = remainingTtl(accessClaims.getExpiration());
        if (ttl.isZero() || ttl.isNegative()) {
            ttl = Duration.ofSeconds(securityProperties.getAccessTokenExpireSeconds());
        }
        redisOps.set(AuthCacheKeys.accessBlacklist(accessClaims.getId()), reason, ttl);
    }

    public void blacklistAccessByJti(String accessJti, String reason, Duration ttl) {
        if (accessJti == null || accessJti.isBlank()) {
            return;
        }
        Duration effective = (ttl == null || ttl.isZero() || ttl.isNegative())
                ? Duration.ofSeconds(securityProperties.getAccessTokenExpireSeconds())
                : ttl;
        redisOps.set(AuthCacheKeys.accessBlacklist(accessJti), reason, effective);
    }

    public void revokeRefresh(String refreshJti) {
        if (refreshJti != null && !refreshJti.isBlank()) {
            redisOps.delete(AuthCacheKeys.refreshSession(refreshJti));
        }
    }

    /**
     * Mark refresh as kicked (so /refresh can return 20004), then remove session.
     */
    public void kickRefresh(String refreshJti) {
        if (refreshJti == null || refreshJti.isBlank()) {
            return;
        }
        Duration ttl = Duration.ofSeconds(securityProperties.getRefreshTokenExpireSeconds());
        redisOps.set(AuthCacheKeys.refreshKicked(refreshJti), TokenDenyReason.PLATFORM_KICK, ttl);
        redisOps.delete(AuthCacheKeys.refreshSession(refreshJti));
    }

    public boolean isRefreshKicked(String refreshJti) {
        if (refreshJti == null || refreshJti.isBlank()) {
            return false;
        }
        String reason = redisOps.get(AuthCacheKeys.refreshKicked(refreshJti));
        return TokenDenyReason.isPlatformKick(reason);
    }

    public void storeRefresh(String refreshJti, Long userId, Duration ttl) {
        redisOps.delete(AuthCacheKeys.refreshKicked(refreshJti));
        redisOps.set(AuthCacheKeys.refreshSession(refreshJti), String.valueOf(userId), ttl);
    }

    public boolean isRefreshValid(String refreshJti) {
        return redisOps.hasKey(AuthCacheKeys.refreshSession(refreshJti));
    }

    /**
     * Read online session value {@code accessJti|refreshJti}, or null.
     */
    public String getOnlineSession(Long userId, ClientPlatform platform) {
        return redisOps.get(AuthCacheKeys.userOnline(userId, platform));
    }

    /**
     * Bind new session for platform; kick previous same-platform session;
     * if multi-platform disabled, kick other platforms with {@link TokenDenyReason#PLATFORM_KICK}.
     */
    public void bindLoginSession(Long userId, ClientPlatform platform, String accessJti, String refreshJti) {
        Duration accessTtl = Duration.ofSeconds(securityProperties.getAccessTokenExpireSeconds());
        Duration onlineTtl = Duration.ofSeconds(securityProperties.getRefreshTokenExpireSeconds());

        kickOnlineSession(userId, platform, TokenDenyReason.PLATFORM_KICK, accessTtl);

        if (!securityProperties.isMultiPlatformLoginEnabled()) {
            for (ClientPlatform other : ClientPlatform.values()) {
                if (other == platform) {
                    continue;
                }
                kickOnlineSession(userId, other, TokenDenyReason.PLATFORM_KICK, accessTtl);
            }
        }

        redisOps.set(AuthCacheKeys.userOnline(userId, platform), accessJti + "|" + refreshJti, onlineTtl);
    }

    public void clearOnline(Long userId, ClientPlatform platform) {
        redisOps.delete(AuthCacheKeys.userOnline(userId, platform));
    }

    /**
     * Logout helper: blacklist access (from token or online), revoke refresh, clear online.
     */
    public void logoutSession(Long userId, ClientPlatform platform, String accessJtiFromToken, String refreshJtiFromToken) {
        Duration accessTtl = Duration.ofSeconds(securityProperties.getAccessTokenExpireSeconds());
        String online = (userId != null && platform != null) ? getOnlineSession(userId, platform) : null;
        String onlineAccessJti = null;
        String onlineRefreshJti = null;
        if (online != null && !online.isBlank()) {
            String[] parts = online.split("\\|", 2);
            onlineAccessJti = parts[0];
            onlineRefreshJti = parts.length > 1 ? parts[1] : null;
        }

        String accessJti = (accessJtiFromToken != null && !accessJtiFromToken.isBlank())
                ? accessJtiFromToken
                : onlineAccessJti;
        if (accessJti != null && !accessJti.isBlank()) {
            blacklistAccessByJti(accessJti, TokenDenyReason.LOGOUT, accessTtl);
        }

        String refreshJti = (refreshJtiFromToken != null && !refreshJtiFromToken.isBlank())
                ? refreshJtiFromToken
                : onlineRefreshJti;
        revokeRefresh(refreshJti);

        if (userId != null && platform != null) {
            clearOnline(userId, platform);
        }
    }

    public TokenCheckResult checkAccess(Claims claims) {
        if (claims == null || claims.getSubject() == null) {
            return TokenCheckResult.DENIED;
        }
        String jti = claims.getId();
        if (jti != null) {
            String reason = redisOps.get(AuthCacheKeys.accessBlacklist(jti));
            if (reason != null) {
                if (TokenDenyReason.isPlatformKick(reason)) {
                    return TokenCheckResult.KICKED_BY_OTHER_PLATFORM;
                }
                return TokenCheckResult.DENIED;
            }
        }
        Long userId = Long.valueOf(claims.getSubject());
        if (isUserDisabled(userId)) {
            return TokenCheckResult.USER_DISABLED;
        }
        long jwtVer = readVer(claims);
        long current = currentUserVersion(userId);
        if (jwtVer < current) {
            return TokenCheckResult.DENIED;
        }
        return TokenCheckResult.ALLOWED;
    }

    public boolean isAccessAllowed(Claims claims) {
        return checkAccess(claims) == TokenCheckResult.ALLOWED;
    }

    public static long readVer(Claims claims) {
        Object ver = claims.get("ver");
        if (ver instanceof Number number) {
            return number.longValue();
        }
        if (ver instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    public static ClientPlatform readPlatform(Claims claims) {
        Object p = claims.get("platform");
        if (p == null) {
            return null;
        }
        try {
            return ClientPlatform.from(String.valueOf(p));
        } catch (Exception ex) {
            return null;
        }
    }

    private void kickOnlineSession(Long userId, ClientPlatform platform, String reason, Duration accessTtl) {
        String online = redisOps.get(AuthCacheKeys.userOnline(userId, platform));
        if (online == null || online.isBlank()) {
            return;
        }
        String[] parts = online.split("\\|", 2);
        String oldAccessJti = parts[0];
        String oldRefreshJti = parts.length > 1 ? parts[1] : null;
        blacklistAccessByJti(oldAccessJti, reason, accessTtl);
        kickRefresh(oldRefreshJti);
        redisOps.delete(AuthCacheKeys.userOnline(userId, platform));
    }

    private void clearAllOnline(Long userId) {
        for (ClientPlatform platform : ClientPlatform.values()) {
            String online = redisOps.get(AuthCacheKeys.userOnline(userId, platform));
            if (online != null && !online.isBlank()) {
                String[] parts = online.split("\\|", 2);
                revokeRefresh(parts.length > 1 ? parts[1] : null);
            }
            redisOps.delete(AuthCacheKeys.userOnline(userId, platform));
        }
    }

    private static Duration remainingTtl(Date expiration) {
        if (expiration == null) {
            return Duration.ZERO;
        }
        long seconds = expiration.toInstant().getEpochSecond() - Instant.now().getEpochSecond();
        return Duration.ofSeconds(Math.max(seconds, 0));
    }
}
