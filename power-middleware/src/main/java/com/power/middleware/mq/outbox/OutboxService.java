package com.power.middleware.mq.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.power.middleware.mq.RabbitSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService extends ServiceImpl<OutboxMessageMapper, OutboxMessage> {

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

    private final RabbitSender rabbitSender;

    @Transactional(rollbackFor = Exception.class)
    public void enqueue(String topic, String tag, String payload) {
        OutboxMessage message = new OutboxMessage();
        message.setTopic(topic);
        message.setTag(tag);
        message.setPayload(payload);
        message.setStatus(STATUS_NEW);
        message.setRetryCount(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        save(message);
    }

    @Scheduled(fixedDelayString = "${power.outbox.poll-interval-ms:5000}")
    public void dispatch() {
        List<OutboxMessage> list = list(new LambdaQueryWrapper<OutboxMessage>()
                .eq(OutboxMessage::getStatus, STATUS_NEW)
                .last("limit 50"));
        for (OutboxMessage message : list) {
            try {
                rabbitSender.send(message.getTopic(), message.getTag() == null ? "" : message.getTag(),
                        message.getPayload());
                message.setStatus(STATUS_SENT);
                message.setUpdateTime(LocalDateTime.now());
                updateById(message);
            } catch (Exception ex) {
                log.error("Outbox dispatch failed, id={}", message.getId(), ex);
                message.setRetryCount(message.getRetryCount() == null ? 1 : message.getRetryCount() + 1);
                if (message.getRetryCount() >= 10) {
                    message.setStatus(STATUS_FAILED);
                }
                message.setUpdateTime(LocalDateTime.now());
                updateById(message);
            }
        }
    }
}
