package com.power.system.controller;

import com.power.common.result.R;
import com.power.middleware.mq.outbox.OutboxService;
import com.power.middleware.security.SecurityUtils;
import com.power.system.feign.AuthMeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class DemoController {

    private final AuthMeClient authMeClient;
    private final OutboxService outboxService;

    @GetMapping("/ping")
    @PreAuthorize("@authz.permit('system:demo:ping')")
    public R<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "pong");
        data.put("userId", SecurityUtils.currentUserId());
        data.put("username", SecurityUtils.currentUsername());
        return R.ok(data);
    }

    @GetMapping("/feign-me")
    @PreAuthorize("@authz.permit('system:demo:ping')")
    public R<Map<String, Object>> feignMe() {
        R<Map<String, Object>> remote = authMeClient.me();
        Map<String, Object> data = new HashMap<>();
        data.put("localUser", SecurityUtils.currentUsername());
        data.put("remote", remote);
        return R.ok(data);
    }

    @PostMapping("/outbox-demo")
    @PreAuthorize("@authz.permit('system:demo:ping')")
    public R<Void> outboxDemo() {
        log.info("Enqueue outbox demo by {}", SecurityUtils.currentUsername());
        outboxService.enqueue("power.demo", "demo", "{\"event\":\"system-outbox-demo\"}");
        return R.ok();
    }
}
