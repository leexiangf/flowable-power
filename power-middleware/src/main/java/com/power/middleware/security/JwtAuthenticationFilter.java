package com.power.middleware.security;

import com.power.common.constant.SecurityHeaders;
import com.power.common.model.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;
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
                LoginUser user = jwtTokenProvider.parseAccessToken(token);
                LoginUserAuthentication authentication = new LoginUserAuthentication(user);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                log.warn("JWT parse failed: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
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
