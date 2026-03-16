package com.buurman.service.backoffice;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.domain.TeamMember;
import com.buurman.domain.User;
import com.buurman.domain.identifier.TeamIdentifier;
import com.buurman.domain.identifier.UserIdentifier;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeUserDetailResponse;
import com.buurman.dto.response.backoffice.BackofficeUserResponse;
import com.buurman.dto.response.backoffice.UserTeamMembership;
import com.buurman.repository.TeamMemberRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.repository.UserRepository;
import com.buurman.repository.backoffice.BackofficeUserStatsRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.KeycloakService;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeUserService {

  private final UserRepository userRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TeamRepository teamRepository;
  private final BackofficeUserStatsRepository statsRepository;
  private final KeycloakService keycloakService;
  private final Clock clock;

  @Transactional(readOnly = true)
  public PageResponse<BackofficeUserResponse> listUsers(
      PageRequest pageRequest, @Nullable String search, List<TeamIdentifier> teamIdentifiers) {
    Collection<UUID> filterUserIds = null;
    if (teamIdentifiers != null && !teamIdentifiers.isEmpty()) {
      List<UUID> userIds =
          statsRepository.findUserIdsByTeamIdentifiers(
              teamIdentifiers.stream().map(id -> (Sid) id).toList());
      if (userIds.isEmpty()) {
        return PageResponse.of(List.of(), pageRequest.page(), pageRequest.size(), 0);
      }
      filterUserIds = userIds;
    }

    PaginatedResult<User> result =
        userRepository.findAllPaginated(pageRequest, search, filterUserIds);

    Map<UUID, Integer> teamCountMap = statsRepository.countTeamsPerUser();
    Map<UUID, Integer> demoTeamCountMap = statsRepository.countDemoTeamsPerUser();

    Set<String> activeKeycloakUserIds;
    try {
      activeKeycloakUserIds = keycloakService.getActiveAppUserIds();
    } catch (Exception e) {
      log.warn("Failed to fetch active sessions from Keycloak: {}", e.getMessage());
      activeKeycloakUserIds = Set.of();
    }

    Set<String> finalActiveIds = activeKeycloakUserIds;
    List<BackofficeUserResponse> responses =
        result.items().stream()
            .map(
                user ->
                    toResponse(
                        user,
                        teamCountMap.getOrDefault(user.getId(), 0),
                        demoTeamCountMap.getOrDefault(user.getId(), 0),
                        finalActiveIds.contains(user.getKeycloakId())))
            .toList();

    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @Transactional(readOnly = true)
  public BackofficeUserDetailResponse getUser(UserIdentifier identifier) {
    User user = userRepository.getByIdentifierUnscoped(identifier);

    long teamCount = statsRepository.countTeamsForUser(user.getId());
    long demoTeamCount = statsRepository.countDemoTeamsForUser(user.getId());

    boolean online = false;
    try {
      online = keycloakService.isAppUserOnline(user.getKeycloakId());
    } catch (Exception e) {
      log.warn("Failed to check user session status: {}", e.getMessage());
    }

    // Fetch team memberships
    List<TeamMember> memberships = teamMemberRepository.findAllByUserId(user.getId());
    List<UUID> teamIds = memberships.stream().map(TeamMember::getTeamId).toList();
    Map<UUID, Team> teamsById =
        teamRepository.findByIds(teamIds).stream().collect(toMap(Team::getId, identity()));

    List<UserTeamMembership> teamMemberships =
        memberships.stream()
            .map(
                m -> {
                  Team team = teamsById.get(m.getTeamId());
                  if (team == null) {
                    return null;
                  }
                  return new UserTeamMembership(
                      team.getIdentifier().orElseThrow(),
                      team.getName(),
                      m.getRole().name(),
                      m.isOwner(),
                      team.isDemo(),
                      m.getJoinedAt());
                })
            .filter(Objects::nonNull)
            .toList();

    return new BackofficeUserDetailResponse(
        user.getIdentifier().orElseThrow(),
        user.getEmail(),
        Optional.of(user.getFirstName()),
        Optional.of(user.getLastName()),
        user.getPhone(),
        user.getEmailVerifiedAt().isPresent(),
        user.getDisabledAt().isPresent(),
        online,
        teamCount,
        demoTeamCount,
        user.getCreatedAt(),
        Optional.of(user.getUpdatedAt()),
        teamMemberships);
  }

  @Transactional
  public void disableUser(UserIdentifier identifier, BackofficePrincipal principal) {
    User user = userRepository.getByIdentifierUnscoped(identifier);

    LocalDateTime now = LocalDateTime.now(clock);
    userRepository.updateDisabledAt(user.getId(), now);
    keycloakService.disableUser(user.getKeycloakId());

    log.info(
        "Backoffice user {} disabled user {} ({})",
        principal.getEmail().orElse("unknown"),
        identifier,
        user.getEmail());
  }

  @Transactional
  public void enableUser(UserIdentifier identifier, BackofficePrincipal principal) {
    User user = userRepository.getByIdentifierUnscoped(identifier);

    userRepository.updateDisabledAt(user.getId(), null);
    keycloakService.enableUser(user.getKeycloakId());

    log.info(
        "Backoffice user {} enabled user {} ({})",
        principal.getEmail().orElse("unknown"),
        identifier,
        user.getEmail());
  }

  @Transactional
  public void resetPassword(UserIdentifier identifier, BackofficePrincipal principal) {
    User user = userRepository.getByIdentifierUnscoped(identifier);

    keycloakService.sendPasswordResetEmail(user.getKeycloakId());

    log.info(
        "Backoffice user {} triggered password reset for user {} ({})",
        principal.getEmail().orElse("unknown"),
        identifier,
        user.getEmail());
  }

  private BackofficeUserResponse toResponse(
      User user, long teamCount, long demoTeamCount, boolean online) {
    return new BackofficeUserResponse(
        user.getIdentifier().orElseThrow(),
        user.getEmail(),
        Optional.of(user.getFirstName()),
        Optional.of(user.getLastName()),
        user.getPhone(),
        user.getEmailVerifiedAt().isPresent(),
        user.getDisabledAt().isPresent(),
        online,
        teamCount,
        demoTeamCount,
        user.getCreatedAt(),
        Optional.of(user.getUpdatedAt()));
  }
}
