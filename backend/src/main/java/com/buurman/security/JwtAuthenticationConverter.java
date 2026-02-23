package com.buurman.security;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@SuppressWarnings("StringConcatToTextBlock") // Error Prone 2.47.0 bug crashes on this file
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final UserRepository userRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamRepository teamRepository;
  private final Clock clock;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String keycloakId = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    String name = jwt.getClaimAsString("name");

    // Find or create user
    User user =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseGet(() -> createUserFromJwt(keycloakId, email, name));

    // Find all team memberships
    List<TeamMember> memberships = teamMemberRepository.findAllByUserId(user.getId());

    // Select active membership based on priority: activeTeamId → defaultTeamId → first
    Optional<TeamMember> membershipOpt = selectActiveMembership(user, memberships);

    // Create UserPrincipal (membership can be empty for users without team)
    UserPrincipal principal;
    List<SimpleGrantedAuthority> authorities;

    // Extract realm roles from JWT (realm_access.roles)
    List<SimpleGrantedAuthority> realmAuthorities = extractRealmRoles(jwt);

    boolean emailVerified = user.getEmailVerifiedAt() != null;

    String userIdentifier = Objects.requireNonNull(user.getIdentifier(), "User identifier is null");

    if (membershipOpt.isPresent()) {
      TeamMember membership = membershipOpt.get();
      @Nullable String teamIdentifier =
          teamRepository.findById(membership.getTeamId()).map(Team::getIdentifier).orElse(null);
      principal =
          new UserPrincipal(
              user.getId(),
              userIdentifier,
              keycloakId,
              email,
              name,
              membership.getTeamId(),
              teamIdentifier,
              membership.getRole(),
              membership.isOwner(),
              emailVerified);
      List<SimpleGrantedAuthority> allAuthorities = new ArrayList<>();
      allAuthorities.add(new SimpleGrantedAuthority("ROLE_" + membership.getRole()));
      allAuthorities.addAll(realmAuthorities);
      authorities = Collections.unmodifiableList(allAuthorities);
    } else {
      // User without team membership (e.g., accepting invitation)
      principal =
          new UserPrincipal(
              user.getId(),
              userIdentifier,
              keycloakId,
              email,
              name,
              null,
              null,
              null,
              false,
              emailVerified);
      authorities = realmAuthorities.isEmpty() ? Collections.emptyList() : realmAuthorities;
    }

    return new UserAuthentication(principal, authorities);
  }

  private Optional<TeamMember> selectActiveMembership(User user, List<TeamMember> memberships) {
    if (memberships.isEmpty()) {
      return Optional.empty();
    }

    // Priority 1: User's active team
    UUID activeTeamId = user.getActiveTeamId();
    if (activeTeamId != null) {
      Optional<TeamMember> active =
          memberships.stream().filter(m -> m.getTeamId().equals(activeTeamId)).findFirst();
      if (active.isPresent()) {
        return active;
      }
    }

    // Priority 2: User's default team
    UUID defaultTeamId = user.getDefaultTeamId();
    if (defaultTeamId != null) {
      Optional<TeamMember> defaultMember =
          memberships.stream().filter(m -> m.getTeamId().equals(defaultTeamId)).findFirst();
      if (defaultMember.isPresent()) {
        return defaultMember;
      }
    }

    // Priority 3: First membership (oldest by invited_at)
    return Optional.of(memberships.get(0));
  }

  @SuppressWarnings("unchecked")
  private List<SimpleGrantedAuthority> extractRealmRoles(Jwt jwt) {
    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
    if (realmAccess == null) {
      return Collections.emptyList();
    }
    Object rolesObj = realmAccess.get("roles");
    if (!(rolesObj instanceof Collection<?> roles)) {
      return Collections.emptyList();
    }
    // Only include non-team roles (team roles are handled via membership)
    Set<String> teamRoles = Set.of("TEAM_ADMIN", "TEAM_EDITOR", "TEAM_VIEWER");
    return roles.stream()
        .map(Object::toString)
        .filter(role -> !teamRoles.contains(role))
        .filter(role -> !role.startsWith("default-roles-"))
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .toList();
  }

  private User createUserFromJwt(String keycloakId, String email, String name) {
    User user = new User();
    user.setKeycloakId(keycloakId);
    user.setEmail(email);
    user.setEmailVerifiedAt(clock.instant());

    String[] nameParts = name != null ? name.split(" ", 2) : new String[] {"", ""};
    user.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
    user.setLastName(nameParts.length > 1 ? nameParts[1] : "");

    return userRepository.save(user);
  }
}
