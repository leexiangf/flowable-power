package com.power.middleware.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：业务 Exchange / Queue、延迟重试 Queue、死信 Queue。
 */
@Configuration
@RequiredArgsConstructor
public class RabbitTopology {

    public static final String SYSTEM_EXCHANGE = "power.system";
    public static final String WORKFLOW_EXCHANGE = "power.workflow";

    public static final String DLX_EXCHANGE = "power.dlx";
    public static final String RETRY_EXCHANGE = "power.retry";

    public static final String SYSTEM_QUEUE = "power.system.queue";
    public static final String SYSTEM_RETRY_QUEUE = "power.system.retry";
    public static final String SYSTEM_DLQ = "power.system.dlq";
    public static final String SYSTEM_ROUTING_KEY = "system.event";

    public static final String WORKFLOW_COMPLETED_QUEUE = "power.workflow.completed.queue";
    public static final String WORKFLOW_CANCELLED_QUEUE = "power.workflow.cancelled.queue";
    public static final String WORKFLOW_COMPLETED_RETRY_QUEUE = "power.workflow.retry.completed";
    public static final String WORKFLOW_CANCELLED_RETRY_QUEUE = "power.workflow.retry.cancelled";
    public static final String WORKFLOW_DLQ = "power.workflow.dlq";

    private final RabbitMqProperties rabbitMqProperties;

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange(RETRY_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange systemExchange() {
        return new TopicExchange(SYSTEM_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange workflowExchange() {
        return new TopicExchange(WORKFLOW_EXCHANGE, true, false);
    }

    @Bean
    public Queue systemQueue() {
        return QueueBuilder.durable(SYSTEM_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(SYSTEM_DLQ)
                .build();
    }

    @Bean
    public Queue systemRetryQueue() {
        return QueueBuilder.durable(SYSTEM_RETRY_QUEUE)
                .ttl((int) rabbitMqProperties.getDelayedRetryTtlMs())
                .deadLetterExchange(SYSTEM_EXCHANGE)
                .deadLetterRoutingKey(SYSTEM_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue systemDlq() {
        return QueueBuilder.durable(SYSTEM_DLQ).build();
    }

    @Bean
    public Queue workflowCompletedQueue() {
        return QueueBuilder.durable(WORKFLOW_COMPLETED_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(WORKFLOW_DLQ)
                .build();
    }

    @Bean
    public Queue workflowCancelledQueue() {
        return QueueBuilder.durable(WORKFLOW_CANCELLED_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(WORKFLOW_DLQ)
                .build();
    }

    @Bean
    public Queue workflowCompletedRetryQueue() {
        return QueueBuilder.durable(WORKFLOW_COMPLETED_RETRY_QUEUE)
                .ttl((int) rabbitMqProperties.getDelayedRetryTtlMs())
                .deadLetterExchange(WORKFLOW_EXCHANGE)
                .deadLetterRoutingKey(WorkflowMqBindings.PROCESS_COMPLETED)
                .build();
    }

    @Bean
    public Queue workflowCancelledRetryQueue() {
        return QueueBuilder.durable(WORKFLOW_CANCELLED_RETRY_QUEUE)
                .ttl((int) rabbitMqProperties.getDelayedRetryTtlMs())
                .deadLetterExchange(WORKFLOW_EXCHANGE)
                .deadLetterRoutingKey(WorkflowMqBindings.PROCESS_CANCELLED)
                .build();
    }

    @Bean
    public Queue workflowDlq() {
        return QueueBuilder.durable(WORKFLOW_DLQ).build();
    }

    @Bean
    public Binding systemQueueBinding(TopicExchange systemExchange, Queue systemQueue) {
        return BindingBuilder.bind(systemQueue).to(systemExchange).with(SYSTEM_ROUTING_KEY);
    }

    @Bean
    public Binding workflowCompletedBinding(TopicExchange workflowExchange, Queue workflowCompletedQueue) {
        return BindingBuilder.bind(workflowCompletedQueue).to(workflowExchange)
                .with(WorkflowMqBindings.PROCESS_COMPLETED);
    }

    @Bean
    public Binding workflowCancelledBinding(TopicExchange workflowExchange, Queue workflowCancelledQueue) {
        return BindingBuilder.bind(workflowCancelledQueue).to(workflowExchange)
                .with(WorkflowMqBindings.PROCESS_CANCELLED);
    }

    @Bean
    public Binding systemRetryBinding(DirectExchange retryExchange, Queue systemRetryQueue) {
        return BindingBuilder.bind(systemRetryQueue).to(retryExchange).with(SYSTEM_RETRY_QUEUE);
    }

    @Bean
    public Binding workflowCompletedRetryBinding(DirectExchange retryExchange, Queue workflowCompletedRetryQueue) {
        return BindingBuilder.bind(workflowCompletedRetryQueue).to(retryExchange).with(WORKFLOW_COMPLETED_RETRY_QUEUE);
    }

    @Bean
    public Binding workflowCancelledRetryBinding(DirectExchange retryExchange, Queue workflowCancelledRetryQueue) {
        return BindingBuilder.bind(workflowCancelledRetryQueue).to(retryExchange).with(WORKFLOW_CANCELLED_RETRY_QUEUE);
    }

    @Bean
    public Binding systemDlqBinding(DirectExchange dlxExchange, Queue systemDlq) {
        return BindingBuilder.bind(systemDlq).to(dlxExchange).with(SYSTEM_DLQ);
    }

    @Bean
    public Binding workflowDlqBinding(DirectExchange dlxExchange, Queue workflowDlq) {
        return BindingBuilder.bind(workflowDlq).to(dlxExchange).with(WORKFLOW_DLQ);
    }

    /** workflow routing keys（与 power-workflow 常量对齐） */
    public static final class WorkflowMqBindings {
        public static final String PROCESS_COMPLETED = "process.completed";
        public static final String PROCESS_CANCELLED = "process.cancelled";

        private WorkflowMqBindings() {
        }
    }
}
