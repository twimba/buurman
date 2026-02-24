package com.buurman.service;

import static com.buurman.domain.NotificationType.VERIFICATION_CODE;
import static com.buurman.domain.TeamRole.TEAM_ADMIN;
import static com.buurman.util.FeatureFlags.INVITATION_REQUIRED;
import static com.buurman.util.UlidGenerator.newTeamId;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.EmailVerificationCode;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Team;
import com.buurman.domain.TeamInvitation;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.dto.request.RegisterRequest;
import com.buurman.dto.request.UpdateProfileRequest;
import com.buurman.dto.response.UserResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.VerificationCodeException;
import com.buurman.mapper.UserMapper;
import com.buurman.repository.EmailVerificationCodeRepository;
import com.buurman.repository.TeamInvitationRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthService {

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
  private final RegistrationInvitationService registrationInvitationService;
  private final FeatureFlagService featureFlagService;
  private final MetricsService metricsService;
  private final Clock clock;

  @SuppressWarnings("NullAway.Init")
  @org.springframework.beans.factory.annotation.Value("${app.email.base-url}")
  private String baseUrl;

  public AuthService(
      KeycloakService keycloakService,
      UserRepository userRepository,
      TeamRepository teamRepository,
      TeamMemberRepository teamMemberRepository,
      TeamInvitationRepository invitationRepository,
      EmailVerificationCodeRepository verificationCodeRepository,
      UserMapper userMapper,
      NotificationService notificationService,
      RegistrationInvitationService registrationInvitationService,
      FeatureFlagService featureFlagService,
      MetricsService metricsService,
      Clock clock) {
    this.keycloakService = keycloakService;
    this.userRepository = userRepository;
    this.teamRepository = teamRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.invitationRepository = invitationRepository;
    this.verificationCodeRepository = verificationCodeRepository;
    this.userMapper = userMapper;
    this.notificationService = notificationService;
    this.registrationInvitationService = registrationInvitationService;
    this.featureFlagService = featureFlagService;
    this.metricsService = metricsService;
    this.clock = clock;
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    // Check if invitation code is required
    boolean invitationRequired = featureFlagService.isEnabled(INVITATION_REQUIRED);
    Optional<String> registrationCode =
        request.registrationInvitationCode().filter(s -> !s.isBlank());

    if (invitationRequired) {
      if (registrationCode.isEmpty()) {
        throw new BadRequestException("Invitation code is required");
      }
      if (!registrationInvitationService
          .validateCode(
              registrationCode.orElseThrow(
                  () ->
                      new IllegalStateException("Registration code is empty after isEmpty check")))
          .valid()) {
        throw new BadRequestException("Invalid or expired invitation code");
      }
    }

    // Validate email not already registered
    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalArgumentException("Email already registered");
    }

    // Create user in Keycloak first (external system)
    String keycloakId =
        keycloakService.createUser(
            request.email(), request.firstName(), request.lastName(), request.password());

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
      team.setIdentifier(newTeamId().value());
      team.setName(teamName);
      team.setCreatedAt(clock.instant());
      team.setUpdatedAt(clock.instant());
      team.setCreatedBy(user.getId());
      team = teamRepository.save(team);

      // Set user's default and active team
      user.setDefaultTeamId(Optional.of(team.getId()));
      user.setActiveTeamId(Optional.of(team.getId()));
      user = userRepository.save(user);

      // Add user as team admin and owner
      TeamMember member = new TeamMember();
      member.setTeamId(team.getId());
      member.setUserId(user.getId());
      member.setRole(TEAM_ADMIN);
      member.setOwner(true);
      member.setInvitedAt(clock.instant());
      member.setJoinedAt(clock.instant());
      teamMemberRepository.save(member);

      // Record registration invitation usage atomically
      if (invitationRequired && !registrationCode.get().isBlank()) {
        registrationInvitationService.recordUsage(registrationCode.get(), user.getId());
      }

      // Send verification code email
      String code = generateVerificationCode();
      createAndSendVerificationCode(user, code);

      // Auto-accept invitation if token provided
      final User finalUser = user;
      request
          .invitationToken()
          .filter(t -> !t.isBlank())
          .ifPresent(token -> acceptInvitationForNewUser(token, finalUser));

      metricsService.incrementCounter("team.registered.total");
      metricsService.incrementCounter("keycloak.user.creation.total", "result", "success");

      return userMapper.toResponse(user, team.getIdentifier(), TEAM_ADMIN.name());
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
    User user = userRepository.getById(userId);

    // Get active membership based on user's activeTeamId
    Optional<TeamMember> member = getActiveMembership(user);
    String teamIdentifier = resolveTeamIdentifier(member).orElse(null);

    return userMapper.toResponse(
        user, teamIdentifier, member.map(m -> m.getRole().name()).orElse(null));
  }

  @Transactional
  public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
    User user = userRepository.getById(userId);

    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user = userRepository.save(user);

    Optional<TeamMember> member = getActiveMembership(user);
    String teamIdentifier = resolveTeamIdentifier(member).orElse(null);

    return userMapper.toResponse(
        user, teamIdentifier, member.map(m -> m.getRole().name()).orElse(null));
  }

  private Optional<String> resolveTeamIdentifier(Optional<TeamMember> member) {
    return member.flatMap(m -> teamRepository.findById(m.getTeamId()).map(Team::getIdentifier));
  }

  private Optional<TeamMember> getActiveMembership(User user) {
    java.util.List<TeamMember> memberships = teamMemberRepository.findAllByUserId(user.getId());
    if (memberships.isEmpty()) {
      return Optional.empty();
    }

    // Priority: activeTeamId → defaultTeamId → first membership
    Optional<TeamMember> activeMatch =
        user.getActiveTeamId()
            .flatMap(id -> memberships.stream().filter(m -> m.getTeamId().equals(id)).findFirst());
    if (activeMatch.isPresent()) {
      return activeMatch;
    }

    Optional<TeamMember> defaultMatch =
        user.getDefaultTeamId()
            .flatMap(id -> memberships.stream().filter(m -> m.getTeamId().equals(id)).findFirst());
    if (defaultMatch.isPresent()) {
      return defaultMatch;
    }

    return Optional.of(memberships.getFirst());
  }

  @Transactional
  public UserResponse verifyEmail(UUID userId, String code) {
    User user = userRepository.getById(userId);

    if (user.getEmailVerifiedAt().isPresent()) {
      throw new VerificationCodeException("Email is already verified");
    }

    EmailVerificationCode validCode =
        verificationCodeRepository
            .findValidCode(userId, code)
            .orElseThrow(
                () -> new VerificationCodeException("Invalid or expired verification code"));

    verificationCodeRepository.markUsed(validCode.getId());
    userRepository.updateEmailVerifiedAt(userId);

    // Send welcome notification now that email is verified
    user.setEmailVerifiedAt(Optional.of(clock.instant()));
    user.getActiveTeamId()
        .ifPresent(
            welcomeTeamId ->
                notificationService.send(
                    SendNotificationRequest.builder()
                        .teamId(welcomeTeamId)
                        .notificationType(NotificationType.WELCOME)
                        .recipientUserId(user.getId())
                        .recipientEmail(user.getEmail())
                        .recipientPhone(user.getPhone().orElse(null))
                        .templateName("welcome")
                        .templateVariables(
                            Map.of("userName", user.getFirstName(), "baseUrl", baseUrl))
                        .build()));

    Optional<TeamMember> member = getActiveMembership(user);
    String teamIdentifier = resolveTeamIdentifier(member).orElse(null);
    return userMapper.toResponse(
        user, teamIdentifier, member.map(m -> m.getRole().name()).orElse(null));
  }

  @Transactional
  public void resendVerificationCode(UUID userId) {
    User user = userRepository.getById(userId);

    if (user.getEmailVerifiedAt().isPresent()) {
      throw new VerificationCodeException("Email is already verified");
    }

    int recentCount =
        verificationCodeRepository.countRecentByUserId(userId, clock.instant().minus(1, HOURS));
    if (recentCount >= MAX_RESEND_PER_HOUR) {
      throw new VerificationCodeException(
          "Too many verification attempts. Please try again later.");
    }

    verificationCodeRepository.invalidateAllForUser(userId);
    String code = generateVerificationCode();
    createAndSendVerificationCode(user, code);
  }

  private void createAndSendVerificationCode(User user, String code) {
    EmailVerificationCode verificationCode = new EmailVerificationCode();
    verificationCode.setUserId(user.getId());
    verificationCode.setCode(code);
    verificationCode.setExpiresAt(clock.instant().plus(VERIFICATION_CODE_EXPIRY_MINUTES, MINUTES));
    verificationCodeRepository.save(verificationCode);

    user.getActiveTeamId()
        .ifPresent(
            verificationTeamId ->
                notificationService.send(
                    SendNotificationRequest.builder()
                        .teamId(verificationTeamId)
                        .notificationType(VERIFICATION_CODE)
                        .recipientUserId(user.getId())
                        .recipientEmail(user.getEmail())
                        .recipientPhone(user.getPhone().orElse(null))
                        .templateName("verification-code")
                        .templateVariables(
                            Map.of(
                                "userName",
                                user.getFirstName(),
                                "verificationCode",
                                code,
                                "expiresMinutes",
                                15))
                        .build()));
  }

  private void acceptInvitationForNewUser(String token, User user) {
    try {
      TeamInvitation invitation = invitationRepository.findByToken(token).orElse(null);
      if (invitation == null) {
        log.warn("Invitation token not found during registration: {}", token);
        return;
      }

      if (invitation.getAcceptedAt().isPresent()
          || invitation.getExpiresAt().isBefore(clock.instant())) {
        log.warn("Invitation already accepted or expired during registration: {}", token);
        return;
      }

      if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
        log.warn(
            "Invitation email mismatch during registration: expected={}, got={}",
            invitation.getEmail(),
            user.getEmail());
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
      invitation.setAcceptedAt(Optional.of(clock.instant()));
      invitation.setAcceptedBy(Optional.of(user.getId()));
      invitationRepository.save(invitation);

      // Switch to the invited team as default and active
      user.setDefaultTeamId(Optional.of(invitation.getTeamId()));
      user.setActiveTeamId(Optional.of(invitation.getTeamId()));
      userRepository.save(user);

      log.info(
          "Auto-accepted invitation for new user: email={}, teamId={}",
          user.getEmail(),
          invitation.getTeamId());
    } catch (Exception e) {
      // Don't fail registration if invitation acceptance fails
      log.error("Failed to auto-accept invitation during registration", e);
    }
  }

  private String generateVerificationCode() {
    return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
  }

  /**
   * Generate team name from user's first and last name in kebab-case format. Example:
   * firstName="Luis", lastName="Santos" -> "luis-santos-team"
   */
  private String generateTeamName(String firstName, String lastName) {
    String fullName = (firstName + " " + lastName).trim();
    return fullName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "")
        + "-team";
  }
}
