package com.power.system.mq;

import com.power.middleware.mq.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox 投递调度：仅在 power-system 运行，避免 auth/workflow 等多实例重复轮询同一表。
 */
@Component
@ConditionalOnProperty(prefix = "power.outbox", name = "dispatch-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxDispatchScheduler {

    private final OutboxService outboxService;

    @Scheduled(fixedDelayString = "${power.outbox.poll-interval-ms:5000}")
    public void dispatch() {
        outboxService.dispatch();
    }
}
