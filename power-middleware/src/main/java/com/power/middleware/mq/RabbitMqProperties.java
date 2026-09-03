package com.power.middleware.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RabbitMQ 高可用相关配置。
 */
@Data
@ConfigurationProperties(prefix = "power.rabbitmq")
public class RabbitMqProperties {

    /** 是否启用 Publisher Confirm（Outbox 投递建议开启） */
    private boolean publisherConfirmEnabled = true;

    /** 消费者 prefetch */
    private int prefetch = 10;

    /** 消费者并发数 */
    private int concurrentConsumers = 2;

    /** 消费者最大并发数 */
    private int maxConcurrentConsumers = 5;

    /** 监听器重试次数（含首次） */
    private int maxAttempts = 3;

    /** 重试初始间隔 ms */
    private long initialRetryIntervalMs = 1000;

    /** 重试倍数 */
    private double retryMultiplier = 2.0;

    /** 重试最大间隔 ms */
    private long maxRetryIntervalMs = 10000;

    /** 延迟重试队列 TTL ms（经 DLX 回到业务队列） */
    private long delayedRetryTtlMs = 30000;

    /** 延迟重试最大次数（消息头 x-retry-count） */
    private int maxDelayedRetries = 3;

    /** Outbox 投递最大重试次数 */
    private int outboxMaxRetries = 10;
}
