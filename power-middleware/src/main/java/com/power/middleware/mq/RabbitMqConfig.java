package com.power.middleware.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitTemplate 确认模式与监听器容器（手动 ACK，业务侧自行处理重试/DLQ）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    private final RabbitMqProperties rabbitMqProperties;

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        if (rabbitMqProperties.isPublisherConfirmEnabled()) {
            template.setConfirmCallback((correlation, ack, cause) -> {
                if (!ack) {
                    log.error("Publisher confirm NACK, correlation={}, cause={}", correlation, cause);
                }
            });
            template.setReturnsCallback(returned ->
                    log.error("Message returned: exchange={}, routingKey={}, reply={}",
                            returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
            template.setMandatory(true);
        }
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setPrefetchCount(rabbitMqProperties.getPrefetch());
        factory.setConcurrentConsumers(rabbitMqProperties.getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(rabbitMqProperties.getMaxConcurrentConsumers());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
