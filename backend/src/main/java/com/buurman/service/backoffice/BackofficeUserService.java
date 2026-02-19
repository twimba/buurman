package com.buurman.service.backoffice;

import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;
import static org.jooq.impl.DSL.count;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.User;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeUserResponse;
import com.buurman.repository.UserRepository;
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
  private final KeycloakService keycloakService;
  private final DSLContext dsl;
  private final Clock clock;

  @Transactional(readOnly = true)
  public PageResponse<BackofficeUserResponse> listUsers(PageRequest pageRequest, String search) {
    PaginatedResult<User> result = userRepository.findAllPaginated(pageRequest, search);

    Map<UUID, Integer> teamCountMap =
        dsl.select(TEAM_MEMBERS.USER_ID, count())
            .from(TEAM_MEMBERS)
            .where(TEAM_MEMBERS.DELETED_AT.isNull())
            .groupBy(TEAM_MEMBERS.USER_ID)
            .fetchMap(TEAM_MEMBERS.USER_ID, count());

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
                        finalActiveIds.contains(user.getKeycloakId())))
            .toList();

    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  @Transactional(readOnly = true)
  public BackofficeUserResponse getUser(String identifier) {
    User user = userRepository.getByIdentifierUnscoped(identifier);

    long teamCount =
        dsl.selectCount()
            .from(TEAM_MEMBERS)
            .where(TEAM_MEMBERS.USER_ID.eq(user.getId()).and(TEAM_MEMBERS.DELETED_AT.isNull()))
            .fetchOne(0, long.class);

    boolean online = false;
    try {
      online = keycloakService.isAppUserOnline(user.getKeycloakId());
    } catch (Exception e) {
      log.warn("Failed to check user session status: {}", e.getMessage());
    }

    return toResponse(user, teamCount, online);
  }

  @Transactional
  public void disableUser(String identifier, BackofficePrincipal principal) {
    User user = userRepository.getByIdentifierUnscoped(identifier);

    LocalDateTime now = LocalDateTime.now(clock);
    userRepository.updateDisabledAt(user.getId(), now);
    keycloakService.disableUser(user.getKeycloakId());

    log.info(
        "Backoffice user {} disabled user {} ({})",
        principal.getEmail(),
        identifier,
        user.getEmail());
  }

  @Transactional
  public void enableUser(String identifier, BackofficePrincipal principal) {
    User user = userRepository.getByIdentifierUnscoped(identifier);

    userRepository.updateDisabledAt(user.getId(), null);
    keycloakService.enableUser(user.getKeycloakId());

    log.info(
        "Backoffice user {} enabled user {} ({})",
        principal.getEmail(),
        identifier,
        user.getEmail());
  }

  @Transactional
  public void resetPassword(String identifier, BackofficePrincipal principal) {
    User user = userRepository.getByIdentifierUnscoped(identifier);

    keycloakService.sendPasswordResetEmail(user.getKeycloakId());

    log.info(
        "Backoffice user {} triggered password reset for user {} ({})",
        principal.getEmail(),
        identifier,
        user.getEmail());
  }

  private BackofficeUserResponse toResponse(User user, long teamCount, boolean online) {
    return new BackofficeUserResponse(
        user.getIdentifier(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getPhone(),
        user.getEmailVerifiedAt() != null,
        user.getDisabledAt() != null,
        online,
        teamCount,
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
