package com.power.middleware.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Redis 分布式锁（SET NX + Lua 安全释放）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private static final String UNLOCK_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            else
              return 0
            end
            """;

    private static final String LOCK_PREFIX = "power:lock:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisEnhancementProperties properties;

    /**
     * 在锁保护下执行；获取失败立即返回 null。
     */
    public <T> T execute(String lockKey, Supplier<T> action) {
        return execute(lockKey, properties.getLockLease(), action);
    }

    public <T> T execute(String lockKey, Duration lease, Supplier<T> action) {
        String token = UUID.randomUUID().toString();
        String key = LOCK_PREFIX + lockKey;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, lease);
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("Lock busy: {}", lockKey);
            return null;
        }
        try {
            return action.get();
        } finally {
            unlock(key, token);
        }
    }

    public void unlock(String fullKey, String token) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        stringRedisTemplate.execute(script, Collections.singletonList(fullKey), token);
    }
}
