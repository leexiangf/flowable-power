package com.power.gateway.security;

import com.power.common.constant.AuthCacheKeys;
import com.power.common.constant.TokenCheckResult;
import com.power.common.constant.TokenDenyReason;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Gateway-side Redis checks (same keys as TokenSessionService). No DB access.
 */
@Component
@RequiredArgsConstructor
public class GatewayTokenSessionChecker {

    private final ReactiveStringRedisTemplate redisTemplate;

    public Mono<TokenCheckResult> checkAccess(Claims claims) {
        if (claims == null || claims.getSubject() == null) {
            return Mono.just(TokenCheckResult.DENIED);
        }
        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            return Mono.just(TokenCheckResult.DENIED);
        }

        String jti = claims.getId();
        Mono<TokenCheckResult> blacklistResult = (jti == null || jti.isBlank())
                ? Mono.just(TokenCheckResult.ALLOWED)
                : redisTemplate.opsForValue().get(AuthCacheKeys.accessBlacklist(jti))
                .map(reason -> TokenDenyReason.isPlatformKick(reason)
                        ? TokenCheckResult.KICKED_BY_OTHER_PLATFORM
                        : TokenCheckResult.DENIED)
                .defaultIfEmpty(TokenCheckResult.ALLOWED);

        Mono<Boolean> disabled = redisTemplate.hasKey(AuthCacheKeys.userDisabled(userId))
                .map(Boolean.TRUE::equals);

        long jwtVer = readVer(claims);
        Mono<Boolean> versionOk = redisTemplate.opsForValue().get(AuthCacheKeys.userTokenVersion(userId))
                .defaultIfEmpty("0")
                .map(raw -> {
                    try {
                        return jwtVer >= Long.parseLong(raw);
                    } catch (NumberFormatException ex) {
                        return jwtVer >= 0;
                    }
                });

        return Mono.zip(blacklistResult, disabled, versionOk)
                .map(tuple -> {
                    TokenCheckResult bl = tuple.getT1();
                    if (bl != TokenCheckResult.ALLOWED) {
                        return bl;
                    }
                    if (Boolean.TRUE.equals(tuple.getT2())) {
                        return TokenCheckResult.USER_DISABLED;
                    }
                    if (!Boolean.TRUE.equals(tuple.getT3())) {
                        return TokenCheckResult.DENIED;
                    }
                    return TokenCheckResult.ALLOWED;
                });
    }

    private static long readVer(Claims claims) {
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
}
