package com.buurman.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class BackofficeJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String keycloakId = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    String name = jwt.getClaimAsString("name");

    List<SimpleGrantedAuthority> authorities = extractRealmRoles(jwt);

    String role =
        authorities.stream()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .filter(r -> r.equals("BACKOFFICE_ADMIN"))
            .findFirst()
            .orElse(null);

    BackofficePrincipal principal = new BackofficePrincipal(keycloakId, email, name, role);
    return new BackofficeAuthentication(principal, authorities);
  }

  private List<SimpleGrantedAuthority> extractRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess == null) {
      return Collections.emptyList();
    }
    Object rolesObj = realmAccess.get("roles");
    if (!(rolesObj instanceof Collection<?> roles)) {
      return Collections.emptyList();
    }
    return roles.stream()
        .map(Object::toString)
        .filter(role -> !role.startsWith("default-roles-"))
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .toList();
  }
}
