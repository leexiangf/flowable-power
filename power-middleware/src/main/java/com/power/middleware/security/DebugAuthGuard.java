package com.power.middleware.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebugAuthGuard {

    private final SecurityProperties securityProperties;
    private final Environment environment;

    @PostConstruct
    public void validate() {
        boolean prodLike = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "staging".equalsIgnoreCase(p));
        if (prodLike && securityProperties.isDebugAuthEnabled()) {
            throw new IllegalStateException(
                    "power.security.debug-auth-enabled must be false in prod/staging profiles");
        }
        if (securityProperties.isDebugAuthEnabled()) {
            log.warn("Debug auth is ENABLED (X-Debug-Auth). Do not use in production.");
        }
    }
}
