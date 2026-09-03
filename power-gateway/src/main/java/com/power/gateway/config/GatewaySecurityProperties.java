package com.power.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "power.security")
public class GatewaySecurityProperties {

    private String jwtSecret = "flowable-power-local-jwt-secret-change-me-32bytes";

    private List<String> whitelist = new ArrayList<>(List.of(
            "/auth/login",
            "/auth/login/web",
            "/auth/login/mobile",
            "/auth/refresh",
            "/auth/logout",
            "/actuator/health"
    ));

    private boolean debugAuthEnabled = false;

    private String debugAuthToken = "local-only-change-me";

    private DebugUser debugUser = new DebugUser();

    @Data
    public static class DebugUser {
        private Long userId = 0L;
        private String username = "debug";
    }
}
