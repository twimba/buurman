package com.buurman.service.backoffice;

import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.backoffice.CreateBuurmyRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BuurmyResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.KeycloakService;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class BackofficeBuurmyService {

    private static final Logger log = LoggerFactory.getLogger(BackofficeBuurmyService.class);

    private final KeycloakService keycloakService;

    public BackofficeBuurmyService(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    public PageResponse<BuurmyResponse> listBuurmies(PageRequest pageRequest, String search) {
        int totalElements = keycloakService.countRealmUsers(search);
        List<UserRepresentation> users = keycloakService.listRealmUsers(
                search, pageRequest.offset(), pageRequest.size());

        List<BuurmyResponse> responses = users.stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), totalElements);
    }

    public BuurmyResponse getBuurmy(String keycloakUserId) {
        UserRepresentation user = keycloakService.getRealmUser(keycloakUserId);
        BuurmyResponse response = toResponse(user);

        // Enrich with last session access (only reflects active sessions)
        List<UserSessionRepresentation> sessions = keycloakService.getUserSessions(keycloakUserId);
        Instant lastLogin = sessions.stream()
                .map(UserSessionRepresentation::getLastAccess)
                .max(Comparator.naturalOrder())
                .map(Instant::ofEpochMilli)
                .orElse(null);

        return new BuurmyResponse(
                response.id(), response.username(), response.email(),
                response.firstName(), response.lastName(),
                response.enabled(), response.emailVerified(),
                response.createdAt(), lastLogin
        );
    }

    public BuurmyResponse createBuurmy(CreateBuurmyRequest request, BackofficePrincipal principal) {
        String keycloakUserId = keycloakService.createRealmUser(
                request.email(), request.username(),
                request.firstName(), request.lastName(),
                request.password(), request.temporaryPassword());

        log.info("Backoffice user {} created buurmy {} ({})",
                principal.getEmail(), keycloakUserId, request.email());

        return getBuurmy(keycloakUserId);
    }

    public void disableBuurmy(String keycloakUserId, BackofficePrincipal principal) {
        validateNotSelf(keycloakUserId, principal);
        keycloakService.disableUser(keycloakUserId);
        log.info("Backoffice user {} disabled buurmy {}", principal.getEmail(), keycloakUserId);
    }

    public void enableBuurmy(String keycloakUserId, BackofficePrincipal principal) {
        validateNotSelf(keycloakUserId, principal);
        keycloakService.enableUser(keycloakUserId);
        log.info("Backoffice user {} enabled buurmy {}", principal.getEmail(), keycloakUserId);
    }

    public void deleteBuurmy(String keycloakUserId, BackofficePrincipal principal) {
        validateNotSelf(keycloakUserId, principal);
        keycloakService.deleteUser(keycloakUserId);
        log.info("Backoffice user {} deleted buurmy {}", principal.getEmail(), keycloakUserId);
    }

    public void forcePasswordUpdate(String keycloakUserId, BackofficePrincipal principal) {
        keycloakService.executeUserActions(keycloakUserId, List.of("UPDATE_PASSWORD"));
        log.info("Backoffice user {} forced password update for buurmy {}",
                principal.getEmail(), keycloakUserId);
    }

    public void forceProfileUpdate(String keycloakUserId, BackofficePrincipal principal) {
        keycloakService.executeUserActions(keycloakUserId, List.of("UPDATE_PROFILE"));
        log.info("Backoffice user {} forced profile update for buurmy {}",
                principal.getEmail(), keycloakUserId);
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
                null
        );
    }

    private Instant toInstant(Long epochMillis) {
        return epochMillis != null && epochMillis > 0 ? Instant.ofEpochMilli(epochMillis) : null;
    }
}
