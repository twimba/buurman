package com.buurman.service;

import com.buurman.domain.EmailVerificationCode;
import com.buurman.domain.Team;
import com.buurman.domain.TeamInvitation;
import com.buurman.domain.TeamMember;
import com.buurman.domain.NotificationType;
import com.buurman.domain.User;
import com.buurman.dto.request.RegisterRequest;
import com.buurman.dto.request.UpdateProfileRequest;
import com.buurman.dto.response.UserResponse;
import com.buurman.exception.VerificationCodeException;
import com.buurman.mapper.UserMapper;
import com.buurman.repository.EmailVerificationCodeRepository;
import com.buurman.repository.TeamInvitationRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int VERIFICATION_CODE_EXPIRY_MINUTES = 15;
    private static final int MAX_RESEND_PER_HOUR = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final KeycloakService keycloakService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository invitationRepository;
    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final MetricsService metricsService;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Value("${app.email.base-url}")
    private String baseUrl;

    public AuthService(KeycloakService keycloakService, UserRepository userRepository,
                      TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
                      TeamInvitationRepository invitationRepository,
                      EmailVerificationCodeRepository verificationCodeRepository,
                      UserMapper userMapper, NotificationService notificationService,
                      MetricsService metricsService, Clock clock) {
        this.keycloakService = keycloakService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.invitationRepository = invitationRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.metricsService = metricsService;
        this.clock = clock;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Validate email not already registered
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create user in Keycloak first (external system)
        String keycloakId = keycloakService.createUser(
            request.email(),
            request.firstName(),
            request.lastName(),
            request.password()
        );

        try {
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
            team.setIdentifier(UlidGenerator.generate(EntityPrefix.TEA));
            team.setName(teamName);
            team.setCreatedAt(clock.instant());
            team.setUpdatedAt(clock.instant());
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
            member.setInvitedAt(clock.instant());
            member.setJoinedAt(clock.instant());
            teamMemberRepository.save(member);

            // Send verification code email
            String code = generateVerificationCode();
            createAndSendVerificationCode(user, code);

            // Auto-accept invitation if token provided
            if (request.invitationToken() != null && !request.invitationToken().isBlank()) {
                acceptInvitationForNewUser(request.invitationToken(), user);
            }

            metricsService.incrementCounter("team.registered.total");
            metricsService.incrementCounter("keycloak.user.creation.total", "result", "success");

            return userMapper.toResponse(user, team.getIdentifier(), "TEAM_ADMIN");
        } catch (Exception e) {
            // Compensate: remove orphaned Keycloak user if DB operations fail
            log.error("Registration failed after Keycloak user creation, compensating", e);
            try {
                keycloakService.deleteUser(keycloakId);
            } catch (Exception compensationEx) {
                log.error("Failed to delete orphaned Keycloak user: {}", keycloakId, compensationEx);
            }
            throw e;
        }
    }

    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Get active membership based on user's activeTeamId
        TeamMember member = getActiveMembership(user);
        String teamIdentifier = resolveTeamIdentifier(member);

        return userMapper.toResponse(user, teamIdentifier,
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
        String teamIdentifier = resolveTeamIdentifier(member);

        return userMapper.toResponse(user, teamIdentifier,
                                     member != null ? member.getRole() : null);
    }

    private String resolveTeamIdentifier(TeamMember member) {
        if (member == null) {
            return null;
        }
        return teamRepository.findById(member.getTeamId())
            .map(Team::getIdentifier)
            .orElse(null);
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

    @Transactional
    public UserResponse verifyEmail(UUID userId, String code) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmailVerifiedAt() != null) {
            throw new VerificationCodeException("Email is already verified");
        }

        EmailVerificationCode validCode = verificationCodeRepository.findValidCode(userId, code)
            .orElseThrow(() -> new VerificationCodeException("Invalid or expired verification code"));

        verificationCodeRepository.markUsed(validCode.getId());
        userRepository.updateEmailVerifiedAt(userId);

        // Send welcome notification now that email is verified
        user.setEmailVerifiedAt(clock.instant());
        notificationService.send(SendNotificationRequest.builder()
                .teamId(user.getActiveTeamId())
                .notificationType(NotificationType.WELCOME)
                .recipientUserId(user.getId())
                .recipientEmail(user.getEmail())
                .recipientPhone(user.getPhone())
                .templateName("welcome")
                .templateVariables(Map.of(
                        "userName", user.getFirstName(),
                        "baseUrl", baseUrl
                ))
                .build());

        TeamMember member = getActiveMembership(user);
        String teamIdentifier = resolveTeamIdentifier(member);
        return userMapper.toResponse(user, teamIdentifier,
                                     member != null ? member.getRole() : null);
    }

    @Transactional
    public void resendVerificationCode(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmailVerifiedAt() != null) {
            throw new VerificationCodeException("Email is already verified");
        }

        int recentCount = verificationCodeRepository.countRecentByUserId(
            userId, clock.instant().minus(1, ChronoUnit.HOURS));
        if (recentCount >= MAX_RESEND_PER_HOUR) {
            throw new VerificationCodeException("Too many verification attempts. Please try again later.");
        }

        verificationCodeRepository.invalidateAllForUser(userId);
        String code = generateVerificationCode();
        createAndSendVerificationCode(user, code);
    }

    private void createAndSendVerificationCode(User user, String code) {
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUserId(user.getId());
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(clock.instant().plus(VERIFICATION_CODE_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        verificationCodeRepository.save(verificationCode);

        notificationService.send(SendNotificationRequest.builder()
                .teamId(user.getActiveTeamId())
                .notificationType(NotificationType.VERIFICATION_CODE)
                .recipientUserId(user.getId())
                .recipientEmail(user.getEmail())
                .recipientPhone(user.getPhone())
                .templateName("verification-code")
                .templateVariables(Map.of(
                        "userName", user.getFirstName(),
                        "verificationCode", code,
                        "expiresMinutes", 15
                ))
                .build());
    }

    private void acceptInvitationForNewUser(String token, User user) {
        try {
            TeamInvitation invitation = invitationRepository.findByToken(token).orElse(null);
            if (invitation == null) {
                log.warn("Invitation token not found during registration: {}", token);
                return;
            }

            if (invitation.getAcceptedAt() != null || invitation.getExpiresAt().isBefore(clock.instant())) {
                log.warn("Invitation already accepted or expired during registration: {}", token);
                return;
            }

            if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
                log.warn("Invitation email mismatch during registration: expected={}, got={}", invitation.getEmail(), user.getEmail());
                return;
            }

            // Create team membership
            TeamMember member = new TeamMember();
            member.setTeamId(invitation.getTeamId());
            member.setUserId(user.getId());
            member.setRole(invitation.getRole());
            member.setOwner(false);
            member.setInvitedAt(invitation.getInvitedAt());
            member.setInvitedBy(invitation.getInvitedBy());
            member.setJoinedAt(clock.instant());
            teamMemberRepository.save(member);

            // Mark invitation as accepted
            invitation.setAcceptedAt(clock.instant());
            invitation.setAcceptedBy(user.getId());
            invitationRepository.save(invitation);

            // Switch to the invited team as default and active
            user.setDefaultTeamId(invitation.getTeamId());
            user.setActiveTeamId(invitation.getTeamId());
            userRepository.save(user);

            log.info("Auto-accepted invitation for new user: email={}, teamId={}", user.getEmail(), invitation.getTeamId());
        } catch (Exception e) {
            // Don't fail registration if invitation acceptance fails
            log.error("Failed to auto-accept invitation during registration", e);
        }
    }

    private String generateVerificationCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
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
