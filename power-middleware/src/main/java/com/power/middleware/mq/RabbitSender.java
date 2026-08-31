package com.power.middleware.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitSender {

    private final RabbitTemplate rabbitTemplate;

    public void send(String exchange, String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }
}
