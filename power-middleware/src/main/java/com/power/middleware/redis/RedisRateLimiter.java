package com.power.middleware.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 固定窗口计数限流（Redis INCR + EXPIRE）。
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final String RATE_PREFIX = "power:rate:";

    private final RedisOps redisOps;
    private final RedisEnhancementProperties properties;

    /**
     * 是否允许通过。
     *
     * @param key 限流维度（如 userId / IP）
     * @return true 未超限
     */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, properties.getRateLimitMaxRequests(), properties.getRateLimitWindow());
    }

    public boolean tryAcquire(String key, long maxRequests, Duration window) {
        String redisKey = RATE_PREFIX + key;
        Long count = redisOps.increment(redisKey);
        if (count != null && count == 1L) {
            redisOps.expire(redisKey, window);
        }
        return count != null && count <= maxRequests;
    }
}
