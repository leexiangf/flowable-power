package com.power.middleware.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.power.common.constant.ErrorCode;
import com.power.common.constant.SecurityHeaders;
import com.power.common.constant.TokenCheckResult;
import com.power.common.model.LoginUser;
import com.power.common.result.R;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
    private final TokenSessionService tokenSessionService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod()) || isWhitelisted(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (tryDebugAuth(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(SecurityHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(SecurityHeaders.BEARER_PREFIX)) {
            String token = header.substring(SecurityHeaders.BEARER_PREFIX.length());
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                if ("refresh".equals(claims.get("type"))) {
                    writeUnauthorized(response, ErrorCode.AUTH_TOKEN_INVALID);
                    return;
                } else {
                    TokenCheckResult check = tokenSessionService.checkAccess(claims);
                    if (check == TokenCheckResult.KICKED_BY_OTHER_PLATFORM) {
                        writeUnauthorized(response, ErrorCode.AUTH_KICKED_BY_OTHER_PLATFORM);
                        return;
                    }
                    if (check == TokenCheckResult.USER_DISABLED) {
                        writeUnauthorized(response, ErrorCode.AUTH_USER_DISABLED);
                        return;
                    }
                    if (check != TokenCheckResult.ALLOWED) {
                        log.warn("Access rejected by Redis session check, userId={}, jti={}, result={}",
                                claims.getSubject(), claims.getId(), check);
                        writeUnauthorized(response, ErrorCode.AUTH_TOKEN_INVALID);
                        return;
                    }
                    LoginUser user = jwtTokenProvider.parseAccessToken(token);
                    LoginUserAuthentication authentication = new LoginUserAuthentication(user);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                log.warn("JWT parse failed: {}", ex.getMessage());
                writeUnauthorized(response, ErrorCode.AUTH_TOKEN_INVALID);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), R.fail(errorCode));
    }

    private boolean tryDebugAuth(HttpServletRequest request) {
        if (!securityProperties.isDebugAuthEnabled()) {
            return false;
        }
        String token = request.getHeader(SecurityHeaders.DEBUG_AUTH);
        if (token == null || !token.equals(securityProperties.getDebugAuthToken())) {
            return false;
        }
        SecurityProperties.DebugUser debugUser = securityProperties.getDebugUser();
        LoginUser user = new LoginUser();
        user.setUserId(debugUser.getUserId());
        user.setUsername(debugUser.getUsername());
        user.setAuthorities(debugUser.getAuthorities());
        user.setDebug(true);
        LoginUserAuthentication authentication = new LoginUserAuthentication(user);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.warn("Debug auth used, path={}, user={}", request.getRequestURI(), user.getUsername());
        return true;
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        for (String pattern : securityProperties.getWhitelist()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
