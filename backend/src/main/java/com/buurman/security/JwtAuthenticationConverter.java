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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

        if (membership != null) {
            principal = new UserPrincipal(
                user.getId(),
                keycloakId,
                email,
                name,
                membership.getTeamId(),
                membership.getRole(),
                membership.isOwner()
            );
            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + membership.getRole()));
        } else {
            // User without team membership (e.g., accepting invitation)
            principal = new UserPrincipal(
                user.getId(),
                keycloakId,
                email,
                name,
                null,
                null,
                false
            );
            authorities = Collections.emptyList();
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

    private User createUserFromJwt(String keycloakId, String email, String name) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);

        String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
        user.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");

        return userRepository.save(user);
    }
}
