package com.power.middleware.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 简单字符串缓存（带 TTL）。
 */
@Component
@RequiredArgsConstructor
public class RedisCacheHelper {

    private static final String CACHE_PREFIX = "power:cache:";

    private final RedisOps redisOps;
    private final RedisEnhancementProperties properties;

    public String get(String key) {
        return redisOps.get(CACHE_PREFIX + key);
    }

    public void put(String key, String value) {
        put(key, value, properties.getDefaultCacheTtl());
    }

    public void put(String key, String value, Duration ttl) {
        redisOps.set(CACHE_PREFIX + key, value, ttl);
    }

    public void evict(String key) {
        redisOps.delete(CACHE_PREFIX + key);
    }

    /**
     * cache-aside：命中返回，未命中加载并写入。
     */
    public String getOrLoad(String key, Supplier<String> loader) {
        String cached = get(key);
        if (cached != null) {
            return cached;
        }
        String value = loader.get();
        if (value != null) {
            put(key, value);
        }
        return value;
    }
}
