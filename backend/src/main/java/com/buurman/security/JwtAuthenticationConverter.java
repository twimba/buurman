package com.buurman.security;

import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    public JwtAuthenticationConverter(UserRepository userRepository,
                                     TeamMemberRepository teamMemberRepository) {
        this.userRepository = userRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        // Find or create user
        User user = userRepository.findByKeycloakId(keycloakId)
            .orElseGet(() -> createUserFromJwt(keycloakId, email, name));

        // Find all team memberships
        List<TeamMember> memberships = teamMemberRepository.findAllByUserId(user.getId());

        // Select active membership based on priority: activeTeamId → defaultTeamId → first
        TeamMember membership = selectActiveMembership(user, memberships);

        // Create UserPrincipal (membership can be null for users without team)
        UserPrincipal principal;
        List<SimpleGrantedAuthority> authorities;

        // Extract realm roles from JWT (realm_access.roles)
        List<SimpleGrantedAuthority> realmAuthorities = extractRealmRoles(jwt);

        boolean emailVerified = user.getEmailVerifiedAt() != null;

        if (membership != null) {
            principal = new UserPrincipal(
                user.getId(),
                keycloakId,
                email,
                name,
                membership.getTeamId(),
                membership.getRole(),
                membership.isOwner(),
                emailVerified
            );
            List<SimpleGrantedAuthority> allAuthorities = new ArrayList<>();
            allAuthorities.add(new SimpleGrantedAuthority("ROLE_" + membership.getRole()));
            allAuthorities.addAll(realmAuthorities);
            authorities = Collections.unmodifiableList(allAuthorities);
        } else {
            // User without team membership (e.g., accepting invitation)
            principal = new UserPrincipal(
                user.getId(),
                keycloakId,
                email,
                name,
                null,
                null,
                false,
                emailVerified
            );
            authorities = realmAuthorities.isEmpty() ? Collections.emptyList() : realmAuthorities;
        }

        return new UserAuthentication(principal, authorities);
    }

    private TeamMember selectActiveMembership(User user, List<TeamMember> memberships) {
        if (memberships.isEmpty()) {
            return null;
        }

        // Priority 1: User's active team
        UUID activeTeamId = user.getActiveTeamId();
        if (activeTeamId != null) {
            TeamMember active = memberships.stream()
                .filter(m -> m.getTeamId().equals(activeTeamId))
                .findFirst()
                .orElse(null);
            if (active != null) {
                return active;
            }
        }

        // Priority 2: User's default team
        UUID defaultTeamId = user.getDefaultTeamId();
        if (defaultTeamId != null) {
            TeamMember defaultMember = memberships.stream()
                .filter(m -> m.getTeamId().equals(defaultTeamId))
                .findFirst()
                .orElse(null);
            if (defaultMember != null) {
                return defaultMember;
            }
        }

        // Priority 3: First membership (oldest by invited_at)
        return memberships.get(0);
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
        user.setEmailVerifiedAt(java.time.Instant.now());

        String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
        user.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");

        return userRepository.save(user);
    }
}
