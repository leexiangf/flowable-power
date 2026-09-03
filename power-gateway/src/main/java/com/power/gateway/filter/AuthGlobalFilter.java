package com.power.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.constant.SecurityHeaders;
import com.power.common.constant.TokenCheckResult;
import com.power.common.result.R;
import com.power.common.trace.TraceContext;
import com.power.gateway.config.GatewaySecurityProperties;
import com.power.gateway.security.GatewayTokenSessionChecker;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
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
    private final GatewayTokenSessionChecker tokenSessionChecker;
    private final ObjectMapper objectMapper;
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

        // CORS preflight — let CorsWebFilter / downstream handle, do not require JWT
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            exchange.getResponse().getHeaders().set(TraceContext.HEADER_TRACE_ID, finalTraceId);
            return chain.filter(exchange.mutate().request(mutate.build()).build());
        }

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
            return writeUnauthorized(exchange, ErrorCode.UNAUTHORIZED);
        }

        try {
            String token = authorization.substring(SecurityHeaders.BEARER_PREFIX.length());
            Claims claims = parseClaims(token);
            if ("refresh".equals(claims.get("type"))) {
                return writeUnauthorized(exchange, ErrorCode.AUTH_TOKEN_INVALID);
            }
            return tokenSessionChecker.checkAccess(claims)
                    .flatMap(result -> {
                        if (result == TokenCheckResult.KICKED_BY_OTHER_PLATFORM) {
                            return writeUnauthorized(exchange, ErrorCode.AUTH_KICKED_BY_OTHER_PLATFORM);
                        }
                        if (result == TokenCheckResult.USER_DISABLED) {
                            return writeUnauthorized(exchange, ErrorCode.AUTH_USER_DISABLED);
                        }
                        if (result != TokenCheckResult.ALLOWED) {
                            log.warn("Gateway Redis session reject, userId={}, jti={}, result={}",
                                    claims.getSubject(), claims.getId(), result);
                            return writeUnauthorized(exchange, ErrorCode.AUTH_TOKEN_INVALID);
                        }
                        ServerHttpRequest.Builder ok = mutate
                                .header(SecurityHeaders.USER_ID, claims.getSubject())
                                .header(SecurityHeaders.USERNAME, claims.get("username", String.class))
                                .header(HttpHeaders.AUTHORIZATION, authorization);
                        exchange.getResponse().getHeaders().set(TraceContext.HEADER_TRACE_ID, finalTraceId);
                        return chain.filter(exchange.mutate().request(ok.build()).build());
                    });
        } catch (Exception ex) {
            log.warn("Gateway JWT invalid: {}", ex.getMessage());
            return writeUnauthorized(exchange, ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(R.fail(errorCode));
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":" + errorCode.getCode() + ",\"message\":\"" + errorCode.getMessage() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
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
