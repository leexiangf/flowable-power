package com.power.middleware.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redis 增强能力配置。
 */
@Data
@ConfigurationProperties(prefix = "power.redis")
public class RedisEnhancementProperties {

    /** 默认缓存 TTL */
    private Duration defaultCacheTtl = Duration.ofMinutes(30);

    /** 分布式锁默认租约 */
    private Duration lockLease = Duration.ofSeconds(30);

    /** 限流窗口 */
    private Duration rateLimitWindow = Duration.ofMinutes(1);

    /** 限流窗口内默认最大请求数 */
    private long rateLimitMaxRequests = 100;
}
