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
 * power.demo 示例消费者：手动 ACK + Redis 幂等 + 延迟重试 + DLQ。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "power.rabbitmq.consumer", name = "demo-enabled", havingValue = "true", matchIfMissing = true)
public class DemoMessageListener extends AbstractManualAckListener {

    private final MessageRetrySupport retrySupport;
    private final MessageIdempotencyService idempotencyService;

    @RabbitListener(queues = RabbitTopology.DEMO_QUEUE, ackMode = "MANUAL")
    public void onDemoMessage(Message message, Channel channel) throws IOException {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        handleWithRetry(message, channel, retrySupport,
                RabbitTopology.DEMO_RETRY_QUEUE,
                RabbitTopology.DEMO_DLQ,
                () -> {
                    String idemKey = message.getMessageProperties().getMessageId();
                    if (idemKey == null) {
                        idemKey = "demo:" + body.hashCode();
                    }
                    if (!idempotencyService.tryMark(idemKey, Duration.ofDays(7))) {
                        log.info("Skip duplicate demo message id={}", idemKey);
                        return;
                    }
                    log.info("Demo MQ consumed: {}", body);
                });
    }
}
