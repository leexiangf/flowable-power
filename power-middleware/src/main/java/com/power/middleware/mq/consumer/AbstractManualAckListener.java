package com.power.middleware.mq.consumer;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;

import java.io.IOException;

/**
 * 消费端 ACK / 延迟重试 / DLQ 模板方法。
 */
public abstract class AbstractManualAckListener {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected void handleWithRetry(Message message,
                                   Channel channel,
                                   MessageRetrySupport retrySupport,
                                   String retryRoutingKey,
                                   String dlqRoutingKey,
                                   Runnable business) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            business.run();
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.error("Consume failed, queue={}, routingKey={}",
                    message.getMessageProperties().getConsumerQueue(),
                    message.getMessageProperties().getReceivedRoutingKey(), ex);
            if (retrySupport.scheduleDelayedRetry(retryRoutingKey, dlqRoutingKey, message)) {
                channel.basicAck(tag, false);
            } else {
                channel.basicReject(tag, false);
            }
        }
    }
}
