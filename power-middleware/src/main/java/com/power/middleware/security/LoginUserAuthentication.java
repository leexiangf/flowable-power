package com.power.middleware.security;

import com.power.common.model.LoginUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LoginUserAuthentication extends AbstractAuthenticationToken {

    private final LoginUser principal;

    public LoginUserAuthentication(LoginUser principal) {
        super(toAuthorities(principal.getAuthorities()));
        this.principal = principal;
        setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> toAuthorities(List<String> authorities) {
        if (authorities == null) {
            return List.of();
        }
        return authorities.stream()
                .filter(a -> a != null && !a.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public LoginUser getPrincipal() {
        return principal;
    }
}
