package com.power.system.controller;

import com.power.common.result.R;
import com.power.middleware.mq.RabbitTopology;
import com.power.middleware.mq.outbox.OutboxService;
import com.power.middleware.security.SecurityUtils;
import com.power.system.feign.AuthMeClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统联调探针：权限、Feign、Outbox。
 */
@Tag(name = "系统探针", description = "权限、Feign、Outbox 联调接口")
@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemProbeController {

    private final AuthMeClient authMeClient;
    private final OutboxService outboxService;

    @Operation(summary = "Ping", description = "校验登录态与权限码 system:probe:ping，返回当前用户摘要。")
    @GetMapping("/ping")
    @PreAuthorize("@authz.permit('system:probe:ping')")
    public R<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "pong");
        data.put("userId", SecurityUtils.currentUserId());
        data.put("username", SecurityUtils.currentUsername());
        return R.ok(data);
    }

    @Operation(summary = "Feign 调用 auth/me", description = "本服务经 Feign 调用 power-auth 的 /auth/me，用于验证服务间鉴权头透传。")
    @GetMapping("/feign-me")
    @PreAuthorize("@authz.permit('system:probe:ping')")
    public R<Map<String, Object>> feignMe() {
        R<Map<String, Object>> remote = authMeClient.me();
        Map<String, Object> data = new HashMap<>();
        data.put("localUser", SecurityUtils.currentUsername());
        data.put("remote", remote);
        return R.ok(data);
    }

    @Operation(summary = "Outbox 入队", description = "写入本地出箱表，由定时任务投递到 RabbitMQ（exchange=power.system）。")
    @PostMapping("/outbox")
    @PreAuthorize("@authz.permit('system:probe:ping')")
    public R<Void> enqueueOutbox() {
        log.info("Enqueue system outbox by {}", SecurityUtils.currentUsername());
        outboxService.enqueue(
                RabbitTopology.SYSTEM_EXCHANGE,
                RabbitTopology.SYSTEM_ROUTING_KEY,
                "{\"event\":\"system.outbox\"}");
        return R.ok();
    }
}
