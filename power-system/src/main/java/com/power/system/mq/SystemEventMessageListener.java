package com.power.system.mq;

import com.power.middleware.mq.RabbitTopology;
import com.power.middleware.mq.consumer.AbstractManualAckListener;
import com.power.middleware.mq.consumer.MessageIdempotencyService;
import com.power.middleware.mq.consumer.MessageRetrySupport;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * power.system 消费者：手动 ACK + Redis 幂等 + 延迟重试 + DLQ。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "power.rabbitmq.consumer", name = "system-enabled", havingValue = "true", matchIfMissing = true)
public class SystemEventMessageListener extends AbstractManualAckListener {

    private final MessageRetrySupport retrySupport;
    private final MessageIdempotencyService idempotencyService;

    @RabbitListener(queues = RabbitTopology.SYSTEM_QUEUE, ackMode = "MANUAL")
    public void onSystemEvent(Message message, Channel channel) throws IOException {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        handleWithRetry(message, channel, retrySupport,
                RabbitTopology.SYSTEM_RETRY_QUEUE,
                RabbitTopology.SYSTEM_DLQ,
                () -> {
                    String idemKey = message.getMessageProperties().getMessageId();
                    if (idemKey == null) {
                        idemKey = "system:" + body.hashCode();
                    }
                    if (!idempotencyService.tryMark(idemKey, Duration.ofDays(7))) {
                        log.info("Skip duplicate system message id={}", idemKey);
                        return;
                    }
                    log.info("System MQ consumed: {}", body);
                });
    }
}
