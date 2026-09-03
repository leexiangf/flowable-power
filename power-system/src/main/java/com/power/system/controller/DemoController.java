package com.power.system.controller;

import com.power.common.result.R;
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
 * 系统示例接口：权限、Feign、Outbox 联调。
 */
@Tag(name = "系统示例", description = "权限、Feign、Outbox 联调示例接口")
@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class DemoController {

    private final AuthMeClient authMeClient;
    private final OutboxService outboxService;

    /**
     * 权限与登录态探活。
     *
     * @return 当前用户摘要
     */
    @Operation(summary = "Ping", description = "校验登录态与权限码 system:demo:ping，返回当前用户摘要。")
    @GetMapping("/ping")
    @PreAuthorize("@authz.permit('system:demo:ping')")
    public R<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "pong");
        data.put("userId", SecurityUtils.currentUserId());
        data.put("username", SecurityUtils.currentUsername());
        return R.ok(data);
    }

    /**
     * 经 Feign 调用 auth 的当前用户接口。
     *
     * @return 本地用户与远程结果
     */
    @Operation(summary = "Feign 调用 auth/me", description = "本服务经 Feign 调用 power-auth 的 /auth/me，用于验证服务间鉴权头透传。")
    @GetMapping("/feign-me")
    @PreAuthorize("@authz.permit('system:demo:ping')")
    public R<Map<String, Object>> feignMe() {
        R<Map<String, Object>> remote = authMeClient.me();
        Map<String, Object> data = new HashMap<>();
        data.put("localUser", SecurityUtils.currentUsername());
        data.put("remote", remote);
        return R.ok(data);
    }

    /**
     * 写入一条 Outbox 示例消息。
     *
     * @return 空成功响应
     */
    @Operation(summary = "Outbox 入队示例", description = "写入本地出箱表，由定时任务投递到 RabbitMQ（exchange=power.demo）。")
    @PostMapping("/outbox-demo")
    @PreAuthorize("@authz.permit('system:demo:ping')")
    public R<Void> outboxDemo() {
        log.info("Enqueue outbox demo by {}", SecurityUtils.currentUsername());
        outboxService.enqueue("power.demo", "demo", "{\"event\":\"system-outbox-demo\"}");
        return R.ok();
    }
}
