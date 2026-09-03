package com.power.middleware.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 可靠发送：可选 Publisher Confirm + mandatory return。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitSender {

    private static final long CONFIRM_TIMEOUT_MS = 5000;

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;

    /**
     * 发送消息（不等待 confirm，适合 fire-and-forget 场景）。
     */
    public void send(String exchange, String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }

    /**
     * 发送并等待 Publisher Confirm（Outbox 投递使用）。
     *
     * @throws TimeoutException confirm 超时
     */
    public void sendConfirmed(String exchange, String routingKey, Object payload) throws TimeoutException {
        if (!rabbitMqProperties.isPublisherConfirmEnabled()) {
            send(exchange, routingKey, payload);
            return;
        }
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, payload, correlationData);
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(CONFIRM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (confirm == null || !confirm.isAck()) {
                throw new TimeoutException("Publisher confirm failed for exchange=" + exchange + ", routingKey=" + routingKey);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TimeoutException("Publisher confirm interrupted for exchange=" + exchange);
        } catch (java.util.concurrent.ExecutionException ex) {
            throw new TimeoutException("Publisher confirm error: " + ex.getMessage());
        }
    }
}
