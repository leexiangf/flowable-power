package com.power.middleware.web;

import com.power.common.trace.TraceContext;
import com.power.middleware.security.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Map;

/**
 * 打印 HTTP 接口调试日志：路径、用户、参数、响应摘要、耗时。
 * <p>
 * 通过 {@code power.web.api-log-enabled=true} 开启（仅 local/dev）；生产 profile 默认关闭。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "power.web", name = "api-log-enabled", havingValue = "true")
@RequiredArgsConstructor
public class HttpApiLogFilter extends OncePerRequestFilter {

    private final WebLogProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String pattern : properties.getExcludePaths()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request, properties.getMaxBodyLength());
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startMs = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long costMs = System.currentTimeMillis() - startMs;
            logAccess(wrappedRequest, wrappedResponse, costMs);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logAccess(ContentCachingRequestWrapper request,
                           ContentCachingResponseWrapper response,
                           long costMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query == null ? uri : uri + "?" + query;

        Long userId = SecurityUtils.currentUserId();
        String username = SecurityUtils.currentUsername();
        String user = userId == null ? "-" : username + "(" + userId + ")";

        Map<String, String> params = HttpApiLogSupport.queryParams(request);
        String requestBody = "";
        if (HttpApiLogSupport.shouldLogBody(request.getContentType())) {
            requestBody = HttpApiLogSupport.readCachedBody(
                    request.getContentAsByteArray(),
                    request.getContentType(),
                    request.getCharacterEncoding(),
                    properties.getMaxBodyLength());
        } else if (StringUtils.hasText(request.getContentType())) {
            requestBody = "[" + request.getContentType() + "]";
        }

        String responseBody = "";
        if (HttpApiLogSupport.shouldLogBody(response.getContentType())) {
            responseBody = HttpApiLogSupport.readCachedBody(
                    response.getContentAsByteArray(),
                    response.getContentType(),
                    response.getCharacterEncoding(),
                    properties.getMaxBodyLength());
        } else if (StringUtils.hasText(response.getContentType())) {
            responseBody = "[" + response.getContentType() + "]";
        }

        log.debug(
                "[HTTP] traceId={} | {} {} | user={} | query={} | body={} | status={} | {}ms | response={}",
                TraceContext.getTraceId(),
                method,
                fullPath,
                user,
                params.isEmpty() ? "-" : params,
                StringUtils.hasText(requestBody) ? requestBody : "-",
                response.getStatus(),
                costMs,
                StringUtils.hasText(responseBody) ? responseBody : "-");
    }
}
