package com.power.middleware.mq.consumer;

import com.power.middleware.mq.RabbitMqProperties;
import com.power.middleware.mq.RabbitTopology;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消费失败时：先走 Spring Retry；仍失败则按 x-retry-count 投递延迟重试队列，超限进 DLQ。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRetrySupport {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;

    /**
     * 将消息送入 DLQ。
     */
    public void sendToDlq(String dlqRoutingKey, Message message) {
        rabbitTemplate.send(RabbitTopology.DLX_EXCHANGE, dlqRoutingKey, message);
        log.warn("Message sent to DLQ routingKey={}", dlqRoutingKey);
    }

    /**
     * 递增重试计数并送入延迟重试队列；若已达上限则进 DLQ。
     *
     * @return true 已安排延迟重试；false 已进入 DLQ
     */
    public boolean scheduleDelayedRetry(String retryRoutingKey, String dlqRoutingKey, Message message) {
        Integer retryCount = message.getMessageProperties().getHeader(MqRetryHeaders.RETRY_COUNT);
        int next = retryCount == null ? 1 : retryCount + 1;
        if (next > rabbitMqProperties.getMaxDelayedRetries()) {
            sendToDlq(dlqRoutingKey, message);
            return false;
        }
        message.getMessageProperties().setHeader(MqRetryHeaders.RETRY_COUNT, next);
        rabbitTemplate.send(RabbitTopology.RETRY_EXCHANGE, retryRoutingKey, message);
        log.warn("Scheduled delayed retry #{}/{} routingKey={}",
                next, rabbitMqProperties.getMaxDelayedRetries(), retryRoutingKey);
        return true;
    }
}
