package com.power.middleware.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "power.security")
public class SecurityProperties {

    /** HMAC secret for JWT (local/dev only; use Nacos in shared env). */
    private String jwtSecret = "flowable-power-local-jwt-secret-change-me-32bytes";

    private long accessTokenExpireSeconds = 7200;

    private long refreshTokenExpireSeconds = 604800;

    private List<String> whitelist = new ArrayList<>(List.of(
            "/auth/login",
            "/auth/login/web",
            "/auth/login/mobile",
            "/auth/refresh",
            "/auth/logout",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    ));

    private boolean debugAuthEnabled = false;

    private String debugAuthToken = "local-only-change-me";

    /**
     * When true, WEB and MOBILE (and future platforms) may stay online together.
     * When false, login on one platform kicks sessions on other platforms.
     */
    private boolean multiPlatformLoginEnabled = true;

    private DebugUser debugUser = new DebugUser();

    @Data
    public static class DebugUser {
        private Long userId = 0L;
        private String username = "debug";
        private List<String> authorities = new ArrayList<>(List.of("*"));
    }
}
