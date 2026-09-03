package com.power.middleware.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SpEL: {@code @PreAuthorize("@authz.permit('system:user:list')")}.
 * Supports debug wildcard authority {@code *}.
 */
@Component("authz")
public class Authz {

    public boolean permit(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authority == null) {
            return false;
        }
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            String value = granted.getAuthority();
            if ("*".equals(value) || authority.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /** 任一权限满足即可，用于实例详情等可读接口的多权限场景 */
    public boolean permitAny(String... authorities) {
        if (authorities == null || authorities.length == 0) {
            return false;
        }
        for (String authority : authorities) {
            if (permit(authority)) {
                return true;
            }
        }
        return false;
    }
}
