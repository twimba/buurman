package com.buurman.repository;

import com.buurman.domain.User;
import com.buurman.dto.request.PageRequest;
import com.buurman.jooq.generated.tables.records.UsersRecord;
import com.buurman.mapper.UserRecordMapper;
import com.buurman.util.EntityPrefix;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.buurman.util.UlidGenerator;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.USERS;

@Repository
public class UserRepository {

    private final DSLContext dsl;
    private final UserRecordMapper mapper;

    public UserRepository(DSLContext dsl, UserRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    public Optional<User> findByIdentifier(String identifier) {
        return dsl.selectFrom(USERS)
                .where(USERS.IDENTIFIER.eq(identifier))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Optional<User> findById(UUID id) {
        return dsl.selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public User save(User user) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (user.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            String identifier = UlidGenerator.generate(EntityPrefix.USR);
            dsl.insertInto(USERS)
                    .set(USERS.ID, newId)
                    .set(USERS.IDENTIFIER, identifier)
                    .set(USERS.KEYCLOAK_ID, user.getKeycloakId())
                    .set(USERS.EMAIL, user.getEmail())
                    .set(USERS.FIRST_NAME, user.getFirstName())
                    .set(USERS.LAST_NAME, user.getLastName())
                    .set(USERS.PHONE, user.getPhone())
                    .set(USERS.DEFAULT_TEAM_ID, user.getDefaultTeamId())
                    .set(USERS.ACTIVE_TEAM_ID, user.getActiveTeamId())
                    .set(USERS.EMAIL_VERIFIED_AT, user.getEmailVerifiedAt() != null
                            ? LocalDateTime.ofInstant(user.getEmailVerifiedAt(), ZoneOffset.UTC) : null)
                    .set(USERS.CREATED_AT, now)
                    .set(USERS.UPDATED_AT, now)
                    .execute();

            user.setId(newId);
            user.setIdentifier(identifier);
            user.setCreatedAt(now.toInstant(ZoneOffset.UTC));
            user.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            // UPDATE
            dsl.update(USERS)
                    .set(USERS.KEYCLOAK_ID, user.getKeycloakId())
                    .set(USERS.EMAIL, user.getEmail())
                    .set(USERS.FIRST_NAME, user.getFirstName())
                    .set(USERS.LAST_NAME, user.getLastName())
                    .set(USERS.PHONE, user.getPhone())
                    .set(USERS.DEFAULT_TEAM_ID, user.getDefaultTeamId())
                    .set(USERS.ACTIVE_TEAM_ID, user.getActiveTeamId())
                    .set(USERS.EMAIL_VERIFIED_AT, user.getEmailVerifiedAt() != null
                            ? LocalDateTime.ofInstant(user.getEmailVerifiedAt(), ZoneOffset.UTC) : null)
                    .set(USERS.UPDATED_AT, now)
                    .where(USERS.ID.eq(user.getId()))
                    .execute();

            user.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }

        return user;
    }

    public void softDeleteById(UUID id) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(USERS)
                .set(USERS.DELETED_AT, now)
                .where(USERS.ID.eq(id))
                .execute();
    }

    public List<User> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return dsl.selectFrom(USERS)
                .where(USERS.ID.in(ids))
                .fetch()
                .map(mapper::toDomain);
    }

    public Optional<User> findByKeycloakId(String keycloakId) {
        return dsl.selectFrom(USERS)
                .where(USERS.KEYCLOAK_ID.eq(keycloakId))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Optional<User> findByEmail(String email) {
        return dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public boolean existsByEmail(String email) {
        return dsl.fetchExists(
                dsl.selectFrom(USERS)
                        .where(USERS.EMAIL.eq(email))
        );
    }

    public void updateActiveTeamId(UUID userId, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(USERS)
                .set(USERS.ACTIVE_TEAM_ID, teamId)
                .set(USERS.UPDATED_AT, now)
                .where(USERS.ID.eq(userId))
                .execute();
    }

    public void updateEmailVerifiedAt(UUID userId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(USERS)
                .set(USERS.EMAIL_VERIFIED_AT, now)
                .set(USERS.UPDATED_AT, now)
                .where(USERS.ID.eq(userId))
                .execute();
    }

    public void updateDefaultTeamId(UUID userId, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(USERS)
                .set(USERS.DEFAULT_TEAM_ID, teamId)
                .set(USERS.UPDATED_AT, now)
                .where(USERS.ID.eq(userId))
                .execute();
    }

    public PaginatedResult<User> findAllPaginated(PageRequest pageRequest, String search) {
        Condition condition = USERS.DELETED_AT.isNull();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search + "%";
            condition = condition.and(
                USERS.EMAIL.likeIgnoreCase(pattern)
                .or(USERS.FIRST_NAME.likeIgnoreCase(pattern))
                .or(USERS.LAST_NAME.likeIgnoreCase(pattern))
            );
        }

        Map<String, Field<?>> sortableFields = Map.of(
            "email", USERS.EMAIL,
            "firstName", USERS.FIRST_NAME,
            "lastName", USERS.LAST_NAME,
            "createdAt", USERS.CREATED_AT
        );

        return PaginationHelper.paginate(dsl, USERS, condition, sortableFields,
            USERS.CREATED_AT, pageRequest, r -> mapper.toDomain((UsersRecord) r));
    }

    public Optional<User> findByIdentifierUnscoped(String identifier) {
        return dsl.selectFrom(USERS)
            .where(USERS.IDENTIFIER.eq(identifier))
            .and(USERS.DELETED_AT.isNull())
            .fetchOptional()
            .map(mapper::toDomain);
    }

    public void updateDisabledAt(UUID userId, LocalDateTime disabledAt) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(USERS)
            .set(USERS.DISABLED_AT, disabledAt)
            .set(USERS.UPDATED_AT, now)
            .where(USERS.ID.eq(userId))
            .execute();
    }

    public long countAll() {
        return dsl.selectCount().from(USERS)
            .where(USERS.DELETED_AT.isNull())
            .fetchOne(0, long.class);
    }

    public long countDisabled() {
        return dsl.selectCount().from(USERS)
            .where(USERS.DELETED_AT.isNull().and(USERS.DISABLED_AT.isNotNull()))
            .fetchOne(0, long.class);
    }
}
