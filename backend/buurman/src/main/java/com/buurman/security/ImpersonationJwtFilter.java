package com.buurman.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.buurman.config.ImpersonationProperties;
import com.buurman.domain.ImpersonationMode;
import com.buurman.domain.Sid;
import com.buurman.domain.TeamRole;
import com.buurman.repository.ImpersonationSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ImpersonationJwtFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(ImpersonationJwtFilter.class);
  private static final String IMPERSONATION_ISSUER = "buurman-impersonation";
  private static final long REVOCATION_CACHE_TTL_SECONDS = 15;

  private final ImpersonationProperties properties;
  private final ImpersonationSessionRepository sessionRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** Cache: session UUID string -> expiry instant of the cache entry. */
  private final ConcurrentHashMap<String, Instant> revocationCache = new ConcurrentHashMap<>();

  public ImpersonationJwtFilter(
      ImpersonationProperties properties,
      ImpersonationSessionRepository sessionRepository,
      ObjectMapper objectMapper,
      Clock clock) {
    this.properties = properties;
    this.sessionRepository = sessionRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    // Try parsing as impersonation JWT first; if it fails, let normal OAuth2 handle it
    Claims claims;
    try {
      claims =
          Jwts.parser()
              .verifyWith(properties.signingKey())
              .clock(() -> java.util.Date.from(clock.instant()))
              .requireIssuer(IMPERSONATION_ISSUER)
              .build()
              .parseSignedClaims(token)
              .getPayload();
    } catch (JwtException e) {
      // Not an impersonation token — let the regular OAuth2 filter handle it
      filterChain.doFilter(request, response);
      return;
    }

    // Validate expiration
    Instant expiration = claims.getExpiration().toInstant();
    if (clock.instant().isAfter(expiration)) {
      sendError(response, 401, "Impersonation session expired");
      return;
    }

    // Build ImpersonationPrincipal from claims
    try {
      UUID userId = UUID.fromString(claims.get("user_id", String.class));
      String userIdentifier = claims.get("user_identifier", String.class);
      String keycloakId = claims.get("keycloak_id", String.class);
      String email = claims.get("email", String.class);
      String name = claims.get("name", String.class);
      UUID teamId = UUID.fromString(claims.get("team_id", String.class));
      String teamIdentifier = claims.get("team_identifier", String.class);
      String roleStr = claims.get("role", String.class);
      TeamRole role = roleStr != null ? TeamRole.valueOf(roleStr) : null;
      boolean isOwner = claims.get("is_owner", Boolean.class);
      String sessionId = claims.get("impersonation_session_id", String.class);
      String sessionUuidStr = claims.get("session_uuid", String.class);
      UUID sessionUuid = sessionUuidStr != null ? UUID.fromString(sessionUuidStr) : new UUID(0, 0);
      String adminEmail = claims.get("impersonated_by_email", String.class);
      String adminName = claims.get("impersonated_by_name", String.class);
      String modeStr = claims.get("mode", String.class);
      ImpersonationMode mode = ImpersonationMode.valueOf(modeStr);

      // Revocation check: verify session is still ACTIVE in DB (with cache)
      if (!isSessionActive(sessionUuid)) {
        sendError(response, 401, "Impersonation session has been revoked");
        return;
      }

      ImpersonationPrincipal principal =
          new ImpersonationPrincipal(
              userId,
              userIdentifier,
              keycloakId,
              email,
              name,
              teamId,
              teamIdentifier,
              role,
              isOwner,
              true,
              Sid.of(sessionId),
              sessionUuid,
              adminEmail,
              adminName,
              mode);

      // Build authorities with role hierarchy
      List<SimpleGrantedAuthority> authorities = buildAuthoritiesWithHierarchy(role);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(principal, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Strip the Authorization header so BearerTokenAuthenticationFilter
      // does not attempt to re-validate this HMAC-signed JWT with Keycloak's RSA keys.
      filterChain.doFilter(stripAuthorizationHeader(request), response);
    } catch (Exception e) {
      log.error("Failed to build ImpersonationPrincipal from JWT claims", e);
      sendError(response, 401, "Invalid impersonation token");
    }
  }

  /**
   * Wraps the request to hide the Authorization header, preventing downstream filters from
   * re-processing the impersonation JWT.
   */
  private HttpServletRequest stripAuthorizationHeader(HttpServletRequest request) {
    return new HttpServletRequestWrapper(request) {
      @Override
      @SuppressWarnings("NullAway") // getHeader returns null by contract for missing headers
      public @org.jspecify.annotations.Nullable String getHeader(String name) {
        if ("Authorization".equalsIgnoreCase(name)) {
          return null;
        }
        return super.getHeader(name);
      }

      @Override
      public Enumeration<String> getHeaders(String name) {
        if ("Authorization".equalsIgnoreCase(name)) {
          return Collections.enumeration(Collections.emptyList());
        }
        return super.getHeaders(name);
      }
    };
  }

  private boolean isSessionActive(UUID sessionUuid) {
    String key = sessionUuid.toString();
    Instant now = clock.instant();

    // Check cache
    Instant cachedExpiry = revocationCache.get(key);
    if (cachedExpiry != null && now.isBefore(cachedExpiry)) {
      return true;
    }

    // Check DB
    boolean active = sessionRepository.findActiveSessionById(sessionUuid).isPresent();
    if (active) {
      revocationCache.put(key, now.plusSeconds(REVOCATION_CACHE_TTL_SECONDS));
    } else {
      revocationCache.remove(key);
    }

    return active;
  }

  /**
   * Builds granted authorities including subordinate roles from the role hierarchy. Mirrors Spring
   * Security's RoleHierarchy: TEAM_ADMIN > TEAM_EDITOR > TEAM_VIEWER.
   */
  private List<SimpleGrantedAuthority> buildAuthoritiesWithHierarchy(
      @org.jspecify.annotations.Nullable TeamRole role) {
    if (role == null) {
      return Collections.emptyList();
    }
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
    if (role == TeamRole.TEAM_ADMIN) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + TeamRole.TEAM_EDITOR.name()));
      authorities.add(new SimpleGrantedAuthority("ROLE_" + TeamRole.TEAM_VIEWER.name()));
    } else if (role == TeamRole.TEAM_EDITOR) {
      authorities.add(new SimpleGrantedAuthority("ROLE_" + TeamRole.TEAM_VIEWER.name()));
    }
    return Collections.unmodifiableList(authorities);
  }

  private void sendError(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                Map.of(
                    "timestamp",
                    clock.instant().toString(),
                    "status",
                    status,
                    "error",
                    "IMPERSONATION_ERROR",
                    "message",
                    message)));
  }
}
