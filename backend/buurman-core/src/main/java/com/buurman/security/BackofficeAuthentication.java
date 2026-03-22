package com.buurman.security;

import java.util.Collection;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Custom authentication token that wraps BackofficePrincipal as the principal. This
 * ensures @AuthenticationPrincipal correctly retrieves the BackofficePrincipal object.
 */
public class BackofficeAuthentication extends AbstractAuthenticationToken {

  private final BackofficePrincipal principal;

  public BackofficeAuthentication(
      BackofficePrincipal principal, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public @Nullable Object getCredentials() {
    return null; // No credentials needed for JWT auth
  }

  @Override
  public BackofficePrincipal getPrincipal() {
    return principal;
  }
}
