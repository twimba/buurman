package com.buurman.security;

import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

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

        // Find team membership
        TeamMember membership = teamMemberRepository.findByUserId(user.getId())
            .orElseThrow(() -> new RuntimeException("User not member of any team"));

        // Create UserPrincipal
        UserPrincipal principal = new UserPrincipal(
            user.getId(),
            keycloakId,
            email,
            name,
            membership.getTeamId(),
            membership.getRole()
        );

        // Create authorities with ROLE_ prefix
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + membership.getRole())
        );

        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);
        token.setDetails(principal);
        return token;
    }

    private User createUserFromJwt(String keycloakId, String email, String name) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);

        // Split name into first and last name (handle case where name might not have a space)
        String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
        user.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");

        return userRepository.save(user);
    }
}
