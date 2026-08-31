package com.power.middleware.mq;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String DEMO_EXCHANGE = "power.demo";

    @Bean
    public TopicExchange demoExchange() {
        return new TopicExchange(DEMO_EXCHANGE, true, false);
    }
}
