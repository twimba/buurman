package com.buurman.service.backoffice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.springframework.stereotype.Service;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.CreateBuurmyRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BuurmyResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.KeycloakService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeBuurmyService {

  private final KeycloakService keycloakService;

  public PageResponse<BuurmyResponse> listBuurmies(PageRequest pageRequest, String search) {
    int totalElements = keycloakService.countRealmUsers(search);

    if (totalElements == 0) {
      return PageResponse.of(List.of(), pageRequest.page(), pageRequest.size(), 0);
    }

    // Fetch all matching users for correct sorting across pages
    List<UserRepresentation> allUsers = keycloakService.listRealmUsers(search, 0, totalElements);

    // Sort if requested
    if (pageRequest.sort() != null && !pageRequest.sort().isBlank()) {
      Comparator<UserRepresentation> comparator = getComparator(pageRequest.sort());
      if (pageRequest.direction() == SortDirection.DESC) {
        comparator = comparator.reversed();
      }
      allUsers = new ArrayList<>(allUsers);
      allUsers.sort(comparator);
    }

    // Paginate in Java
    int from = Math.min(pageRequest.offset(), allUsers.size());
    int to = Math.min(from + pageRequest.size(), allUsers.size());
    List<BuurmyResponse> responses =
        allUsers.subList(from, to).stream().map(this::toResponse).toList();

    return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), totalElements);
  }

  public BuurmyResponse getBuurmy(String keycloakUserId) {
    UserRepresentation user = keycloakService.getRealmUser(keycloakUserId);
    BuurmyResponse response = toResponse(user);

    // Enrich with last session access (only reflects active sessions)
    List<UserSessionRepresentation> sessions = keycloakService.getUserSessions(keycloakUserId);
    Instant lastLogin =
        sessions.stream()
            .map(UserSessionRepresentation::getLastAccess)
            .max(Comparator.naturalOrder())
            .map(Instant::ofEpochMilli)
            .orElse(null);

    return new BuurmyResponse(
        response.id(),
        response.username(),
        response.email(),
        response.firstName(),
        response.lastName(),
        response.enabled(),
        response.emailVerified(),
        response.createdAt(),
        lastLogin,
        response.requiredActions());
  }

  public BuurmyResponse createBuurmy(CreateBuurmyRequest request, BackofficePrincipal principal) {
    String keycloakUserId =
        keycloakService.createRealmUser(
            request.email(), request.username(),
            request.firstName(), request.lastName(),
            request.password(), request.temporaryPassword());

    log.info(
        "Backoffice user {} created buurmy {} ({})",
        principal.getEmail(),
        keycloakUserId,
        request.email());

    return getBuurmy(keycloakUserId);
  }

  public void disableBuurmy(String keycloakUserId, BackofficePrincipal principal) {
    validateNotSelf(keycloakUserId, principal);
    keycloakService.disableBackofficeUser(keycloakUserId);
    log.info("Backoffice user {} disabled buurmy {}", principal.getEmail(), keycloakUserId);
  }

  public void enableBuurmy(String keycloakUserId, BackofficePrincipal principal) {
    validateNotSelf(keycloakUserId, principal);
    keycloakService.enableBackofficeUser(keycloakUserId);
    log.info("Backoffice user {} enabled buurmy {}", principal.getEmail(), keycloakUserId);
  }

  public void deleteBuurmy(String keycloakUserId, BackofficePrincipal principal) {
    validateNotSelf(keycloakUserId, principal);
    keycloakService.deleteBackofficeUser(keycloakUserId);
    log.info("Backoffice user {} deleted buurmy {}", principal.getEmail(), keycloakUserId);
  }

  public void forcePasswordUpdate(String keycloakUserId, BackofficePrincipal principal) {
    keycloakService.addRequiredUserAction(keycloakUserId, "UPDATE_PASSWORD");
    log.info(
        "Backoffice user {} forced password update for buurmy {}",
        principal.getEmail(),
        keycloakUserId);
  }

  public void forceProfileUpdate(String keycloakUserId, BackofficePrincipal principal) {
    keycloakService.addRequiredUserAction(keycloakUserId, "UPDATE_PROFILE");
    log.info(
        "Backoffice user {} forced profile update for buurmy {}",
        principal.getEmail(),
        keycloakUserId);
  }

  public void removePasswordReset(String keycloakUserId, BackofficePrincipal principal) {
    keycloakService.removeRequiredUserAction(keycloakUserId, "UPDATE_PASSWORD");
    log.info(
        "Backoffice user {} removed password reset for buurmy {}",
        principal.getEmail(),
        keycloakUserId);
  }

  public void removeProfileReset(String keycloakUserId, BackofficePrincipal principal) {
    keycloakService.removeRequiredUserAction(keycloakUserId, "UPDATE_PROFILE");
    log.info(
        "Backoffice user {} removed profile reset for buurmy {}",
        principal.getEmail(),
        keycloakUserId);
  }

  public void verifyBuurmy(String keycloakUserId, BackofficePrincipal principal) {
    keycloakService.verifyBackofficeUser(keycloakUserId);
    log.info("Backoffice user {} verified buurmy {}", principal.getEmail(), keycloakUserId);
  }

  public void unverifyBuurmy(String keycloakUserId, BackofficePrincipal principal) {
    keycloakService.unverifyBackofficeUser(keycloakUserId);
    log.info("Backoffice user {} unverified buurmy {}", principal.getEmail(), keycloakUserId);
  }

  private void validateNotSelf(String keycloakUserId, BackofficePrincipal principal) {
    if (keycloakUserId.equals(principal.getKeycloakId())) {
      throw new BadRequestException("Cannot perform this action on yourself");
    }
  }

  private BuurmyResponse toResponse(UserRepresentation user) {
    return new BuurmyResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.isEnabled(),
        Boolean.TRUE.equals(user.isEmailVerified()),
        toInstant(user.getCreatedTimestamp()),
        null,
        user.getRequiredActions() != null ? user.getRequiredActions() : List.of());
  }

  private Instant toInstant(Long epochMillis) {
    return epochMillis != null && epochMillis > 0 ? Instant.ofEpochMilli(epochMillis) : null;
  }

  private Comparator<UserRepresentation> getComparator(String field) {
    return switch (field) {
      case "username" ->
          Comparator.comparing(
              UserRepresentation::getUsername, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
      case "firstName", "name" ->
          Comparator.comparing(
              UserRepresentation::getFirstName,
              Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
      case "enabled", "status" -> Comparator.comparing(UserRepresentation::isEnabled);
      case "createdAt" ->
          Comparator.comparing(
              UserRepresentation::getCreatedTimestamp,
              Comparator.nullsLast(Comparator.naturalOrder()));
      default ->
          Comparator.comparing(
              UserRepresentation::getEmail, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    };
  }
}
