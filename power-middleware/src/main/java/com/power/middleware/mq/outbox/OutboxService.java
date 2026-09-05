package com.power.middleware.mq.outbox;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.power.middleware.mq.RabbitMqProperties;

import com.power.middleware.mq.RabbitSender;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;

import java.util.List;

import java.util.concurrent.TimeoutException;



@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService extends ServiceImpl<OutboxMessageMapper, OutboxMessage> {



    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_SENDING = "SENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";


    private static final int STALE_SENDING_MINUTES = 10;



    private final RabbitSender rabbitSender;
    private final RabbitMqProperties rabbitMqProperties;



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



    /**
     * 多实例安全投递：CAS 抢占 → Publisher Confirm → 标记 SENT；失败指数退避式 DB 重试。
     * <p>
     * 由 {@code power-system} 中的调度器调用，避免各业务模块重复轮询。
     */
    public void dispatch() {
        reclaimStaleSending();
        List<OutboxMessage> list = list(new LambdaQueryWrapper<OutboxMessage>()
                .eq(OutboxMessage::getStatus, STATUS_NEW)
                .orderByAsc(OutboxMessage::getId)
                .last("limit 50"));

        for (OutboxMessage message : list) {
            if (!tryClaim(message.getId())) {
                continue;
            }

            try {
                rabbitSender.sendConfirmed(message.getTopic(),
                        message.getTag() == null ? "" : message.getTag(),
                        message.getPayload());
                markStatus(message.getId(), STATUS_SENT, null);
            } catch (TimeoutException | RuntimeException ex) {
                log.error("Outbox dispatch failed, id={}", message.getId(), ex);
                int retry = message.getRetryCount() == null ? 1 : message.getRetryCount() + 1;
                if (retry >= rabbitMqProperties.getOutboxMaxRetries()) {
                    markStatus(message.getId(), STATUS_FAILED, retry);
                } else {
                    update(new LambdaUpdateWrapper<OutboxMessage>()
                            .eq(OutboxMessage::getId, message.getId())
                            .set(OutboxMessage::getStatus, STATUS_NEW)
                            .set(OutboxMessage::getRetryCount, retry)
                            .set(OutboxMessage::getUpdateTime, LocalDateTime.now()));
                }

            }

        }

    }



    private boolean tryClaim(Long id) {
        return update(new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getId, id)
                .eq(OutboxMessage::getStatus, STATUS_NEW)
                .set(OutboxMessage::getStatus, STATUS_SENDING)
                .set(OutboxMessage::getUpdateTime, LocalDateTime.now()));
    }



    private void markStatus(Long id, String status, Integer retryCount) {
        LambdaUpdateWrapper<OutboxMessage> uw = new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getId, id)
                .set(OutboxMessage::getStatus, status)
                .set(OutboxMessage::getUpdateTime, LocalDateTime.now());
        if (retryCount != null) {
            uw.set(OutboxMessage::getRetryCount, retryCount);
        }
        update(uw);

    }



    private void reclaimStaleSending() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STALE_SENDING_MINUTES);
        update(new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getStatus, STATUS_SENDING)
                .lt(OutboxMessage::getUpdateTime, threshold)
                .set(OutboxMessage::getStatus, STATUS_NEW)
                .set(OutboxMessage::getUpdateTime, LocalDateTime.now()));

    }

}


