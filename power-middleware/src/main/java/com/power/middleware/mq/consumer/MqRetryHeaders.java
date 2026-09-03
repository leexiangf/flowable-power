package com.power.middleware.mq.consumer;

/**
 * MQ 延迟重试消息头。
 */
public final class MqRetryHeaders {

    public static final String RETRY_COUNT = "x-retry-count";

    private MqRetryHeaders() {
    }
}
