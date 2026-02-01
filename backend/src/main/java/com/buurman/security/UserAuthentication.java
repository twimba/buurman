package com.buurman.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Custom authentication token that wraps UserPrincipal as the principal.
 * This ensures @AuthenticationPrincipal correctly retrieves the UserPrincipal object.
 */
public class UserAuthentication extends AbstractAuthenticationToken {

    private final UserPrincipal principal;

    public UserAuthentication(UserPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // No credentials needed for JWT auth
    }

    @Override
    public UserPrincipal getPrincipal() {
        return principal;
    }
}
