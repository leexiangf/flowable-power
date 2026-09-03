package com.power.middleware.mq.consumer;

import com.power.middleware.redis.RedisOps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 基于 Redis 的消息幂等（at-least-once 消费去重）。
 */
@Service
@RequiredArgsConstructor
public class MessageIdempotencyService {

    private static final String KEY_PREFIX = "power:mq:idem:";

    private final RedisOps redisOps;

    /**
     * 尝试标记消息已处理。
     *
     * @param idempotencyKey 业务幂等键（如 outboxId / processInstanceId+event）
     * @param ttl            去重窗口
     * @return true 首次处理；false 重复消息应跳过
     */
    public boolean tryMark(String idempotencyKey, Duration ttl) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }
        Boolean ok = redisOps.setIfAbsent(KEY_PREFIX + idempotencyKey, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }
}
