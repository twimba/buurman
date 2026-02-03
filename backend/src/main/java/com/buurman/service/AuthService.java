package com.buurman.service;

import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.dto.request.RegisterRequest;
import com.buurman.dto.request.UpdateProfileRequest;
import com.buurman.dto.response.UserResponse;
import com.buurman.mapper.UserMapper;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.util.UlidGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final KeycloakService keycloakService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserMapper userMapper;
    private final EmailService emailService;

    public AuthService(KeycloakService keycloakService, UserRepository userRepository,
                      TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
                      UserMapper userMapper, EmailService emailService) {
        this.keycloakService = keycloakService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Validate email not already registered
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered");
        }

        // Create user in Keycloak
        String keycloakId = keycloakService.createUser(
            request.email(),
            request.firstName(),
            request.lastName(),
            request.password()
        );

        // Auto-generate team name from user's name in kebab-case format
        String teamName = generateTeamName(request.firstName(), request.lastName());

        // Create user first (need user ID for team.createdBy)
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user = userRepository.save(user);

        // Create team
        Team team = new Team();
        team.setIdentifier(UlidGenerator.generate());
        team.setName(teamName);
        team.setCreatedAt(Instant.now());
        team.setUpdatedAt(Instant.now());
        team.setCreatedBy(user.getId());
        team = teamRepository.save(team);

        // Set user's default and active team
        user.setDefaultTeamId(team.getId());
        user.setActiveTeamId(team.getId());
        user = userRepository.save(user);

        // Add user as team admin and owner
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(user.getId());
        member.setRole("TEAM_ADMIN");
        member.setOwner(true);
        member.setInvitedAt(Instant.now());
        member.setJoinedAt(Instant.now());
        teamMemberRepository.save(member);

        // Send welcome email
        emailService.sendWelcomeEmail(user);

        return userMapper.toResponse(user, team.getId(), "TEAM_ADMIN");
    }

    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Get active membership based on user's activeTeamId
        TeamMember member = getActiveMembership(user);

        return userMapper.toResponse(user, member != null ? member.getTeamId() : null,
                                     member != null ? member.getRole() : null);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user = userRepository.save(user);

        TeamMember member = getActiveMembership(user);

        return userMapper.toResponse(user, member != null ? member.getTeamId() : null,
                                     member != null ? member.getRole() : null);
    }

    private TeamMember getActiveMembership(User user) {
        java.util.List<TeamMember> memberships = teamMemberRepository.findAllByUserId(user.getId());
        if (memberships.isEmpty()) {
            return null;
        }

        // Priority: activeTeamId → defaultTeamId → first membership
        UUID activeTeamId = user.getActiveTeamId();
        if (activeTeamId != null) {
            for (TeamMember m : memberships) {
                if (m.getTeamId().equals(activeTeamId)) {
                    return m;
                }
            }
        }

        UUID defaultTeamId = user.getDefaultTeamId();
        if (defaultTeamId != null) {
            for (TeamMember m : memberships) {
                if (m.getTeamId().equals(defaultTeamId)) {
                    return m;
                }
            }
        }

        return memberships.get(0);
    }

    /**
     * Generate team name from user's first and last name in kebab-case format.
     * Example: firstName="Luis", lastName="Santos" -> "luis-santos-team"
     */
    private String generateTeamName(String firstName, String lastName) {
        String fullName = (firstName + " " + lastName).trim();
        return fullName
            .toLowerCase()
            .replaceAll("\\s+", "-")
            .replaceAll("[^a-z0-9-]", "")
            + "-team";
    }
}
