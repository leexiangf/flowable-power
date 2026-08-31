package com.power.gateway.filter;

import com.power.common.constant.SecurityHeaders;
import com.power.common.trace.TraceContext;
import com.power.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final GatewaySecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        String traceId = request.getHeaders().getFirst(TraceContext.HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        String finalTraceId = traceId;

        ServerHttpRequest.Builder mutate = request.mutate()
                .header(TraceContext.HEADER_TRACE_ID, finalTraceId);

        if (isWhitelisted(path)) {
            exchange.getResponse().getHeaders().set(TraceContext.HEADER_TRACE_ID, finalTraceId);
            return chain.filter(exchange.mutate().request(mutate.build()).build());
        }

        if (securityProperties.isDebugAuthEnabled()) {
            String debugToken = request.getHeaders().getFirst(SecurityHeaders.DEBUG_AUTH);
            if (debugToken != null && debugToken.equals(securityProperties.getDebugAuthToken())) {
                GatewaySecurityProperties.DebugUser debugUser = securityProperties.getDebugUser();
                mutate.header(SecurityHeaders.USER_ID, String.valueOf(debugUser.getUserId()))
                        .header(SecurityHeaders.USERNAME, debugUser.getUsername())
                        .header(SecurityHeaders.DEBUG_AUTH_USED, "1")
                        .header(SecurityHeaders.DEBUG_AUTH, debugToken);
                log.warn("Gateway debug auth used, path={}, user={}", path, debugUser.getUsername());
                exchange.getResponse().getHeaders().set(TraceContext.HEADER_TRACE_ID, finalTraceId);
                return chain.filter(exchange.mutate().request(mutate.build()).build());
            }
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(SecurityHeaders.BEARER_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authorization.substring(SecurityHeaders.BEARER_PREFIX.length());
            Claims claims = parseClaims(token);
            mutate.header(SecurityHeaders.USER_ID, claims.getSubject())
                    .header(SecurityHeaders.USERNAME, claims.get("username", String.class))
                    .header(HttpHeaders.AUTHORIZATION, authorization);
            exchange.getResponse().getHeaders().set(TraceContext.HEADER_TRACE_ID, finalTraceId);
            return chain.filter(exchange.mutate().request(mutate.build()).build());
        } catch (Exception ex) {
            log.warn("Gateway JWT invalid: {}", ex.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isWhitelisted(String path) {
        return securityProperties.getWhitelist().stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
