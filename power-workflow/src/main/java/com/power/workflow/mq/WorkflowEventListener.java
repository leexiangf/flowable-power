package com.power.workflow.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.power.middleware.mq.RabbitTopology;
import com.power.middleware.mq.consumer.AbstractManualAckListener;
import com.power.middleware.mq.consumer.MessageIdempotencyService;
import com.power.middleware.mq.consumer.MessageRetrySupport;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 工作流领域事件消费者（completed / cancelled）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "power.rabbitmq.consumer", name = "workflow-enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowEventListener extends AbstractManualAckListener {

    private final MessageRetrySupport retrySupport;
    private final MessageIdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitTopology.WORKFLOW_COMPLETED_QUEUE, ackMode = "MANUAL")
    public void onCompleted(Message message, Channel channel) throws IOException {
        consume(message, channel,
                RabbitTopology.WORKFLOW_COMPLETED_RETRY_QUEUE,
                RabbitTopology.WorkflowMqBindings.PROCESS_COMPLETED);
    }

    @RabbitListener(queues = RabbitTopology.WORKFLOW_CANCELLED_QUEUE, ackMode = "MANUAL")
    public void onCancelled(Message message, Channel channel) throws IOException {
        consume(message, channel,
                RabbitTopology.WORKFLOW_CANCELLED_RETRY_QUEUE,
                RabbitTopology.WorkflowMqBindings.PROCESS_CANCELLED);
    }

    private void consume(Message message, Channel channel, String retryKey, String eventType) throws IOException {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        handleWithRetry(message, channel, retrySupport, retryKey, RabbitTopology.WORKFLOW_DLQ, () -> {
            try {
                JsonNode node = objectMapper.readTree(body);
                String pi = node.hasNonNull("processInstanceId") ? node.get("processInstanceId").asText() : body;
                String idemKey = eventType + ":" + pi;
                if (!idempotencyService.tryMark(idemKey, Duration.ofDays(30))) {
                    log.info("Skip duplicate workflow event {}", idemKey);
                    return;
                }
                log.info("Workflow event consumed type={}, pi={}, payload={}", eventType, pi, body);
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                throw new IllegalArgumentException("Invalid workflow event JSON", ex);
            }
        });
    }
}
