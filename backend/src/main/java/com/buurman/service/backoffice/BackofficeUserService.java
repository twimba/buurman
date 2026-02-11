package com.buurman.service.backoffice;

import com.buurman.domain.User;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.BackofficeUserResponse;
import com.buurman.repository.UserRepository;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.KeycloakService;
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;

@Service
public class BackofficeUserService {

    private static final Logger log = LoggerFactory.getLogger(BackofficeUserService.class);

    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final DSLContext dsl;

    public BackofficeUserService(UserRepository userRepository,
                                 KeycloakService keycloakService,
                                 DSLContext dsl) {
        this.userRepository = userRepository;
        this.keycloakService = keycloakService;
        this.dsl = dsl;
    }

    @Transactional(readOnly = true)
    public PageResponse<BackofficeUserResponse> listUsers(PageRequest pageRequest, String search) {
        PaginatedResult<User> result = userRepository.findAllPaginated(pageRequest, search);

        Map<UUID, Integer> teamCountMap = dsl.select(TEAM_MEMBERS.USER_ID, DSL.count())
                .from(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.DELETED_AT.isNull())
                .groupBy(TEAM_MEMBERS.USER_ID)
                .fetchMap(TEAM_MEMBERS.USER_ID, DSL.count());

        List<BackofficeUserResponse> responses = result.items().stream()
                .map(user -> toResponse(user, teamCountMap.getOrDefault(user.getId(), 0)))
                .toList();

        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), result.totalElements());
    }

    @Transactional(readOnly = true)
    public BackofficeUserResponse getUser(String identifier) {
        User user = userRepository.findByIdentifierUnscoped(identifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long teamCount = dsl.selectCount()
                .from(TEAM_MEMBERS)
                .where(TEAM_MEMBERS.USER_ID.eq(user.getId())
                        .and(TEAM_MEMBERS.DELETED_AT.isNull()))
                .fetchOne(0, long.class);

        return toResponse(user, teamCount);
    }

    @Transactional
    public void disableUser(String identifier, BackofficePrincipal principal) {
        User user = userRepository.findByIdentifierUnscoped(identifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        userRepository.updateDisabledAt(user.getId(), now);
        keycloakService.disableUser(user.getKeycloakId());

        log.info("Backoffice user {} disabled user {} ({})", principal.getEmail(), identifier, user.getEmail());
    }

    @Transactional
    public void enableUser(String identifier, BackofficePrincipal principal) {
        User user = userRepository.findByIdentifierUnscoped(identifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        userRepository.updateDisabledAt(user.getId(), null);
        keycloakService.enableUser(user.getKeycloakId());

        log.info("Backoffice user {} enabled user {} ({})", principal.getEmail(), identifier, user.getEmail());
    }

    @Transactional
    public void resetPassword(String identifier, BackofficePrincipal principal) {
        User user = userRepository.findByIdentifierUnscoped(identifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        keycloakService.sendPasswordResetEmail(user.getKeycloakId());

        log.info("Backoffice user {} triggered password reset for user {} ({})",
                principal.getEmail(), identifier, user.getEmail());
    }

    private BackofficeUserResponse toResponse(User user, long teamCount) {
        return new BackofficeUserResponse(
                user.getIdentifier(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmailVerifiedAt() != null,
                user.getDisabledAt() != null,
                teamCount,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
