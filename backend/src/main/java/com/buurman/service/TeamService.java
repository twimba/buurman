package com.buurman.service;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Team;
import com.buurman.domain.TeamInvitation;
import com.buurman.domain.TeamMember;
import com.buurman.domain.TeamSettings;
import com.buurman.domain.User;
import com.buurman.dto.request.CreateInvitationRequest;
import com.buurman.dto.request.UpdateMemberRoleRequest;
import com.buurman.dto.request.UpdateTeamRequest;
import com.buurman.dto.request.UpdateTeamSettingsRequest;
import com.buurman.dto.response.InvitationResponse;
import com.buurman.dto.response.TeamMemberResponse;
import com.buurman.dto.response.TeamResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.ForbiddenException;
import com.buurman.mapper.TeamMapper;
import com.buurman.repository.TeamInvitationRepository;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.notification.NotificationService;
import com.buurman.service.notification.SendNotificationRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamInvitationRepository invitationRepository;
  private final UserRepository userRepository;
  private final TeamMapper teamMapper;
  private final NotificationService notificationService;
  private final AppProperties appProperties;
  private final Clock clock;

  public TeamResponse getCurrentTeam(UserPrincipal principal) {
    Team team = teamRepository.getById(principal.getTeamId());

    long memberCount = teamMemberRepository.findByTeamId(team.getId()).size();
    return teamMapper.toResponse(team, memberCount);
  }

  public List<TeamMemberResponse> getTeamMembers(String teamIdentifier, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    // Verify user belongs to this team
    if (!team.getId().equals(principal.getTeamId())) {
      throw new ForbiddenException("Access denied");
    }

    List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());

    // Batch-fetch all users to avoid N+1 queries
    List<UUID> userIds = members.stream().map(TeamMember::getUserId).toList();
    Map<UUID, User> usersById =
        userRepository.findByIds(userIds).stream().collect(toMap(User::getId, u -> u));

    return members.stream()
        .map(
            member ->
                teamMapper.toMemberResponse(
                    member, usersById.get(member.getUserId()), principal.getUserId()))
        .toList();
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public InvitationResponse createInvitation(
      String teamIdentifier, CreateInvitationRequest request, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    // Verify user is admin of this team
    if (!team.getId().equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
      throw new ForbiddenException("Access denied");
    }

    // Check for existing pending invitation for same email + team
    invitationRepository
        .findPendingByEmailAndTeamId(request.email(), team.getId())
        .ifPresent(
            existing -> {
              throw new BusinessRuleException(
                  "A pending invitation already exists for this email address");
            });

    // Create invitation
    TeamInvitation invitation = new TeamInvitation();
    invitation.setTeamId(team.getId());
    invitation.setEmail(request.email());
    invitation.setRole(request.role());
    invitation.setToken(UUID.randomUUID().toString());
    invitation.setExpiresAt(clock.instant().plus(7, DAYS));
    invitation.setInvitedBy(principal.getUserId());
    invitation.setInvitedAt(clock.instant());

    invitation = invitationRepository.save(invitation);

    // Send invitation notification
    String inviterName = principal.getName();
    notificationService.send(
        SendNotificationRequest.builder()
            .teamId(team.getId())
            .notificationType(NotificationType.TEAM_INVITATION)
            .recipientEmail(invitation.getEmail())
            .templateName("team-invitation")
            .templateVariables(
                Map.of(
                    "inviterName", inviterName,
                    "teamName", team.getName(),
                    "role", formatRole(invitation.getRole()),
                    "inviteUrl",
                        appProperties.email().baseUrl() + "/invitation/" + invitation.getToken(),
                    "expiresAt", formatInstantDate(invitation.getExpiresAt())))
            .createdBy(principal.getUserId())
            .build());

    return teamMapper.toInvitationResponse(
        invitation,
        team.getIdentifier(),
        team.getName(),
        inviterName,
        appProperties.email().baseUrl() + "/invitation/");
  }

  public InvitationResponse getInvitation(String token) {
    TeamInvitation invitation = invitationRepository.getByToken(token);

    // Fetch team and inviter details for the response
    Team team = teamRepository.getById(invitation.getTeamId());

    String inviterName = "Team Admin";
    if (invitation.getInvitedBy() != null) {
      User inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);
      if (inviter != null) {
        inviterName = inviter.getFirstName() + " " + inviter.getLastName();
      }
    }

    return teamMapper.toInvitationResponse(
        invitation,
        team.getIdentifier(),
        team.getName(),
        inviterName,
        appProperties.email().baseUrl() + "/invitation/");
  }

  public List<InvitationResponse> getPendingInvitationsForUser(UserPrincipal principal) {
    User user = userRepository.getById(principal.getUserId());

    List<TeamInvitation> pending = invitationRepository.findPendingByEmail(user.getEmail());

    // Exclude teams the user is already a member of
    var memberTeamIds =
        teamMemberRepository.findAllByUserId(principal.getUserId()).stream()
            .map(TeamMember::getTeamId)
            .collect(toSet());

    return pending.stream()
        .filter(inv -> !memberTeamIds.contains(inv.getTeamId()))
        .map(
            invitation -> {
              Team team = teamRepository.findById(invitation.getTeamId()).orElse(null);
              if (team == null) {
                return null;
              }

              String inviterName = "Team Admin";
              if (invitation.getInvitedBy() != null) {
                User inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);
                if (inviter != null) {
                  inviterName = inviter.getFirstName() + " " + inviter.getLastName();
                }
              }

              return teamMapper.toInvitationResponse(
                  invitation,
                  team.getIdentifier(),
                  team.getName(),
                  inviterName,
                  appProperties.email().baseUrl() + "/invitation/");
            })
        .filter(r -> r != null)
        .toList();
  }

  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public List<InvitationResponse> getTeamPendingInvitations(
      String teamIdentifier, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    if (!team.getId().equals(principal.getTeamId())) {
      throw new ForbiddenException("Access denied");
    }

    List<TeamInvitation> pending = invitationRepository.findPendingByTeamId(team.getId());

    return pending.stream()
        .map(
            invitation -> {
              String inviterName = "Team Admin";
              if (invitation.getInvitedBy() != null) {
                User inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);
                if (inviter != null) {
                  inviterName = inviter.getFirstName() + " " + inviter.getLastName();
                }
              }
              return teamMapper.toInvitationResponse(
                  invitation,
                  team.getIdentifier(),
                  team.getName(),
                  inviterName,
                  appProperties.email().baseUrl() + "/invitation/");
            })
        .toList();
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public InvitationResponse resendInvitation(
      String teamIdentifier, String token, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    if (!team.getId().equals(principal.getTeamId())) {
      throw new ForbiddenException("Access denied");
    }

    TeamInvitation invitation = invitationRepository.getByToken(token);

    if (!invitation.getTeamId().equals(team.getId())) {
      throw new BusinessRuleException("Invitation does not belong to this team");
    }

    if (invitation.getAcceptedAt() != null) {
      throw new BusinessRuleException("Invitation already accepted");
    }

    // Reset token, expiry, and resend tracking
    invitation.setToken(UUID.randomUUID().toString());
    invitation.setExpiresAt(clock.instant().plus(7, DAYS));
    invitation.setResentAt(clock.instant());
    invitation.setResentCount(
        invitation.getResentCount() == null ? 1 : invitation.getResentCount() + 1);

    invitation = invitationRepository.save(invitation);

    // Re-send notification email
    String inviterName = principal.getName();
    notificationService.send(
        SendNotificationRequest.builder()
            .teamId(team.getId())
            .notificationType(NotificationType.TEAM_INVITATION)
            .recipientEmail(invitation.getEmail())
            .templateName("team-invitation")
            .templateVariables(
                Map.of(
                    "inviterName", inviterName,
                    "teamName", team.getName(),
                    "role", formatRole(invitation.getRole()),
                    "inviteUrl",
                        appProperties.email().baseUrl() + "/invitation/" + invitation.getToken(),
                    "expiresAt", formatInstantDate(invitation.getExpiresAt())))
            .createdBy(principal.getUserId())
            .build());

    return teamMapper.toInvitationResponse(
        invitation,
        team.getIdentifier(),
        team.getName(),
        inviterName,
        appProperties.email().baseUrl() + "/invitation/");
  }

  @Transactional
  public void acceptInvitation(String token, UserPrincipal principal) {
    TeamInvitation invitation = invitationRepository.getByToken(token);

    // Validate
    if (invitation.getAcceptedAt() != null) {
      throw new BusinessRuleException("Invitation already accepted");
    }
    if (invitation.getExpiresAt().isBefore(clock.instant())) {
      throw new BusinessRuleException("Invitation expired");
    }
    if (!invitation.getEmail().equalsIgnoreCase(principal.getEmail())) {
      throw new BusinessRuleException("Invitation email does not match");
    }

    // Check user not already member of this specific team
    if (teamMemberRepository.existsByTeamIdAndUserId(
        invitation.getTeamId(), principal.getUserId())) {
      throw new BusinessRuleException("User already member of this team");
    }

    // Create team member
    User user = userRepository.getById(principal.getUserId());

    TeamMember member = new TeamMember();
    member.setTeamId(invitation.getTeamId());
    member.setUserId(user.getId());
    member.setRole(invitation.getRole());
    member.setOwner(false); // Invited members are not owners
    member.setInvitedAt(invitation.getInvitedAt());
    member.setInvitedBy(invitation.getInvitedBy());
    member.setJoinedAt(clock.instant());
    teamMemberRepository.save(member);

    // Mark invitation as accepted
    invitation.setAcceptedAt(clock.instant());
    invitation.setAcceptedBy(principal.getUserId());
    invitationRepository.save(invitation);

    // Switch to the invited team as default and active
    user.setDefaultTeamId(invitation.getTeamId());
    user.setActiveTeamId(invitation.getTeamId());
    userRepository.save(user);

    // Notify inviter
    User inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);
    Team team = teamRepository.findById(invitation.getTeamId()).orElse(null);
    if (inviter != null && team != null) {
      notificationService.send(
          SendNotificationRequest.builder()
              .teamId(team.getId())
              .notificationType(NotificationType.INVITATION_ACCEPTED)
              .recipientUserId(inviter.getId())
              .recipientEmail(inviter.getEmail())
              .recipientPhone(inviter.getPhone())
              .templateName("invitation-accepted")
              .templateVariables(
                  Map.of(
                      "inviterName", inviter.getFirstName(),
                      "memberName", user.getFirstName() + " " + user.getLastName(),
                      "memberEmail", user.getEmail(),
                      "teamName", team.getName(),
                      "baseUrl", appProperties.email().baseUrl()))
              .build());
    }
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void removeMember(String teamIdentifier, String userIdentifier, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    // Verify user is admin of this team
    if (!team.getId().equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
      throw new ForbiddenException("Access denied");
    }

    // Resolve user by identifier
    User targetUser = userRepository.getByIdentifier(userIdentifier);

    TeamMember member = teamMemberRepository.getByUserIdAndTeamId(targetUser.getId(), team.getId());

    // Cannot remove self
    if (member.getUserId().equals(principal.getUserId())) {
      throw new BusinessRuleException("Cannot remove yourself");
    }

    teamMemberRepository.softDeleteById(member.getId());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public TeamMemberResponse updateMemberRole(
      String teamIdentifier,
      String userIdentifier,
      UpdateMemberRoleRequest request,
      UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    // Verify user is admin of this team
    if (!team.getId().equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
      throw new ForbiddenException("Access denied");
    }

    // Resolve user by identifier
    User targetUser = userRepository.getByIdentifier(userIdentifier);

    TeamMember member = teamMemberRepository.getByUserIdAndTeamId(targetUser.getId(), team.getId());

    // Cannot change own role
    if (member.getUserId().equals(principal.getUserId())) {
      throw new BusinessRuleException("Cannot change your own role");
    }

    member.setRole(request.role());
    member = teamMemberRepository.save(member);

    return teamMapper.toMemberResponse(member, targetUser, principal.getUserId());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public TeamResponse updateTeam(
      String teamIdentifier, UpdateTeamRequest request, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    // Verify user is admin of this team
    if (!team.getId().equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
      throw new ForbiddenException("Access denied");
    }

    team.setName(request.name());
    team.setUpdatedBy(principal.getUserId());
    team = teamRepository.save(team);

    long memberCount = teamMemberRepository.findByTeamId(team.getId()).size();
    return teamMapper.toResponse(team, memberCount);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public TeamResponse updateTeamSettings(
      String teamIdentifier, UpdateTeamSettingsRequest request, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    // Verify user is admin of this team
    if (!team.getId().equals(principal.getTeamId()) || !"TEAM_ADMIN".equals(principal.getRole())) {
      throw new ForbiddenException("Access denied");
    }

    // Update settings
    TeamSettings settings = team.getSettings();
    if (settings == null) {
      settings = new TeamSettings();
    }

    // Update payment settings
    if (request.payments() != null) {
      TeamSettings.PaymentSettings paymentSettings = new TeamSettings.PaymentSettings();
      paymentSettings.setPaymentsAheadCount(request.payments().paymentsAheadCount());
      paymentSettings.setAutoGenerationEnabled(request.payments().autoGenerationEnabled());
      settings.setPayments(paymentSettings);
    }

    // Update regional settings
    if (request.regional() != null) {
      TeamSettings.RegionalSettings regionalSettings = settings.getRegional();
      if (regionalSettings == null) {
        regionalSettings = new TeamSettings.RegionalSettings();
      }
      if (request.regional().defaultCurrency() != null) {
        regionalSettings.setDefaultCurrency(request.regional().defaultCurrency());
      }
      if (request.regional().defaultCountry() != null) {
        regionalSettings.setDefaultCountry(request.regional().defaultCountry());
      }
      if (request.regional().timezone() != null) {
        regionalSettings.setTimezone(request.regional().timezone());
      }
      if (request.regional().dateFormat() != null) {
        regionalSettings.setDateFormat(request.regional().dateFormat());
      }
      if (request.regional().fiscalYearStartMonth() != null) {
        regionalSettings.setFiscalYearStartMonth(request.regional().fiscalYearStartMonth());
      }
      settings.setRegional(regionalSettings);
    }

    team.setSettings(settings);
    team.setUpdatedBy(principal.getUserId());
    team = teamRepository.save(team);

    long memberCount = teamMemberRepository.findByTeamId(team.getId()).size();
    return teamMapper.toResponse(team, memberCount);
  }

  public TeamSettings getTeamSettings(String teamIdentifier, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    // Verify user belongs to this team
    if (!team.getId().equals(principal.getTeamId())) {
      throw new ForbiddenException("Access denied");
    }

    TeamSettings settings = team.getSettings();
    if (settings == null) {
      settings = new TeamSettings(); // Return defaults
    }

    return settings;
  }

  /**
   * Returns the team's configured default currency (e.g. "EUR", "USD"), or null if not configured.
   */
  public String getDefaultCurrency(UUID teamId) {
    return teamRepository
        .findById(teamId)
        .map(Team::getSettings)
        .map(TeamSettings::getRegional)
        .map(TeamSettings.RegionalSettings::getDefaultCurrency)
        .orElse(null);
  }

  @Transactional
  public TeamMemberResponse transferOwnership(
      String teamIdentifier, String newOwnerIdentifier, UserPrincipal principal) {
    Team team = resolveTeam(teamIdentifier);

    // Verify user is owner of this team
    if (!team.getId().equals(principal.getTeamId()) || !principal.isOwner()) {
      throw new ForbiddenException("Only team owner can transfer ownership");
    }

    // Resolve new owner by identifier
    User newOwnerUser = userRepository.getByIdentifier(newOwnerIdentifier);

    // Cannot transfer to self
    if (newOwnerUser.getId().equals(principal.getUserId())) {
      throw new BusinessRuleException("Cannot transfer ownership to yourself");
    }

    // Find new owner's membership
    TeamMember newOwnerMember =
        teamMemberRepository.getByUserIdAndTeamId(newOwnerUser.getId(), team.getId());

    // Find current owner's membership
    TeamMember currentOwnerMember =
        teamMemberRepository.getByUserIdAndTeamId(principal.getUserId(), team.getId());

    // Transfer ownership
    currentOwnerMember.setOwner(false);
    teamMemberRepository.save(currentOwnerMember);

    newOwnerMember.setOwner(true);
    newOwnerMember.setRole("TEAM_ADMIN"); // Owner must be admin
    newOwnerMember = teamMemberRepository.save(newOwnerMember);

    // Update team's created_by to new owner
    team.setCreatedBy(newOwnerUser.getId());
    team.setUpdatedBy(principal.getUserId());
    teamRepository.save(team);

    return teamMapper.toMemberResponse(newOwnerMember, newOwnerUser, principal.getUserId());
  }

  private Team resolveTeam(String teamIdentifier) {
    return teamRepository.getByIdentifier(teamIdentifier);
  }

  private String formatRole(String role) {
    return switch (role) {
      case "TEAM_ADMIN" -> "Administrator";
      case "TEAM_EDITOR" -> "Editor";
      case "TEAM_VIEWER" -> "Viewer";
      default -> role;
    };
  }

  private String formatInstantDate(Instant instant) {
    return instant != null
        ? LocalDate.ofInstant(instant, UTC).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
        : "";
  }
}
