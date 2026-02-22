package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.USERS;
import static com.buurman.util.UlidGenerator.newUserId;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.User;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.jooq.generated.tables.records.UsersRecord;
import com.buurman.mapper.UserRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepository {

  private final DSLContext dsl;
  private final UserRecordMapper mapper;
  private final Clock clock;

  public Optional<User> findByIdentifier(String identifier) {
    return dsl.selectFrom(USERS)
        .where(USERS.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Optional<User> findById(UUID id) {
    return dsl.selectFrom(USERS).where(USERS.ID.eq(id)).fetchOptional().map(mapper::toDomain);
  }

  public User getById(UUID id) {
    return findById(id).orElseThrow(() -> new NotFoundException("User not found"));
  }

  public User getByIdentifier(String identifier) {
    return findByIdentifier(identifier).orElseThrow(() -> new NotFoundException("User not found"));
  }

  public User save(User user) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (user.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();
      String identifier = newUserId().value();
      LocalDateTime createdAt =
          user.getCreatedAt() != null ? LocalDateTime.ofInstant(user.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          user.getUpdatedAt() != null ? LocalDateTime.ofInstant(user.getUpdatedAt(), UTC) : now;

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
          .set(
              USERS.EMAIL_VERIFIED_AT,
              user.getEmailVerifiedAt() != null
                  ? LocalDateTime.ofInstant(user.getEmailVerifiedAt(), UTC)
                  : null)
          .set(
              USERS.PHONE_VERIFIED_AT,
              user.getPhoneVerifiedAt() != null
                  ? LocalDateTime.ofInstant(user.getPhoneVerifiedAt(), UTC)
                  : null)
          .set(USERS.CREATED_AT, createdAt)
          .set(USERS.UPDATED_AT, updatedAt)
          .execute();

      user.setId(newId);
      user.setIdentifier(identifier);
      user.setCreatedAt(createdAt.toInstant(UTC));
      user.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // UPDATE
      LocalDateTime updatedAt =
          user.getUpdatedAt() != null ? LocalDateTime.ofInstant(user.getUpdatedAt(), UTC) : now;

      dsl.update(USERS)
          .set(USERS.KEYCLOAK_ID, user.getKeycloakId())
          .set(USERS.EMAIL, user.getEmail())
          .set(USERS.FIRST_NAME, user.getFirstName())
          .set(USERS.LAST_NAME, user.getLastName())
          .set(USERS.PHONE, user.getPhone())
          .set(USERS.DEFAULT_TEAM_ID, user.getDefaultTeamId())
          .set(USERS.ACTIVE_TEAM_ID, user.getActiveTeamId())
          .set(
              USERS.EMAIL_VERIFIED_AT,
              user.getEmailVerifiedAt() != null
                  ? LocalDateTime.ofInstant(user.getEmailVerifiedAt(), UTC)
                  : null)
          .set(
              USERS.PHONE_VERIFIED_AT,
              user.getPhoneVerifiedAt() != null
                  ? LocalDateTime.ofInstant(user.getPhoneVerifiedAt(), UTC)
                  : null)
          .set(USERS.UPDATED_AT, updatedAt)
          .where(USERS.ID.eq(user.getId()))
          .execute();

      user.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return user;
  }

  public void softDeleteById(UUID id) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(USERS).set(USERS.DELETED_AT, now).where(USERS.ID.eq(id)).execute();
  }

  public List<User> findByIds(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return List.copyOf(dsl.selectFrom(USERS).where(USERS.ID.in(ids)).fetch().map(mapper::toDomain));
  }

  public Optional<User> findByKeycloakId(String keycloakId) {
    return dsl.selectFrom(USERS)
        .where(USERS.KEYCLOAK_ID.eq(keycloakId))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Optional<User> findByEmail(String email) {
    return dsl.selectFrom(USERS).where(USERS.EMAIL.eq(email)).fetchOptional().map(mapper::toDomain);
  }

  public boolean existsByEmail(String email) {
    return dsl.fetchExists(dsl.selectFrom(USERS).where(USERS.EMAIL.eq(email)));
  }

  public void updateActiveTeamId(UUID userId, @Nullable UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(USERS)
        .set(USERS.ACTIVE_TEAM_ID, teamId)
        .set(USERS.UPDATED_AT, now)
        .where(USERS.ID.eq(userId))
        .execute();
  }

  public void updateEmailVerifiedAt(UUID userId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(USERS)
        .set(USERS.EMAIL_VERIFIED_AT, now)
        .set(USERS.UPDATED_AT, now)
        .where(USERS.ID.eq(userId))
        .execute();
  }

  public void updatePhoneVerifiedAt(UUID userId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(USERS)
        .set(USERS.PHONE_VERIFIED_AT, now)
        .set(USERS.UPDATED_AT, now)
        .where(USERS.ID.eq(userId))
        .execute();
  }

  public void clearPhoneVerifiedAt(UUID userId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(USERS)
        .setNull(USERS.PHONE_VERIFIED_AT)
        .set(USERS.UPDATED_AT, now)
        .where(USERS.ID.eq(userId))
        .execute();
  }

  public void updateDefaultTeamId(UUID userId, @Nullable UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(USERS)
        .set(USERS.DEFAULT_TEAM_ID, teamId)
        .set(USERS.UPDATED_AT, now)
        .where(USERS.ID.eq(userId))
        .execute();
  }

  public PaginatedResult<User> findAllPaginated(PageRequest pageRequest, @Nullable String search) {
    Condition condition = USERS.DELETED_AT.isNull();
    if (search != null && !search.isBlank()) {
      String pattern = "%" + search + "%";
      condition =
          condition.and(
              USERS
                  .EMAIL
                  .likeIgnoreCase(pattern)
                  .or(USERS.FIRST_NAME.likeIgnoreCase(pattern))
                  .or(USERS.LAST_NAME.likeIgnoreCase(pattern))
                  .or(USERS.IDENTIFIER.likeIgnoreCase(pattern)));
    }

    Map<String, Field<?>> sortableFields =
        Map.of(
            "email", USERS.EMAIL,
            "firstName", USERS.FIRST_NAME,
            "lastName", USERS.LAST_NAME,
            "createdAt", USERS.CREATED_AT);

    return PaginationHelper.paginate(
        dsl,
        USERS,
        condition,
        sortableFields,
        USERS.CREATED_AT,
        pageRequest,
        r -> mapper.toDomain((UsersRecord) r));
  }

  public Optional<User> findByIdentifierUnscoped(String identifier) {
    return dsl.selectFrom(USERS)
        .where(USERS.IDENTIFIER.eq(identifier))
        .and(USERS.DELETED_AT.isNull())
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public User getByIdentifierUnscoped(String identifier) {
    return findByIdentifierUnscoped(identifier)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  public void updateDisabledAt(UUID userId, @Nullable LocalDateTime disabledAt) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(USERS)
        .set(USERS.DISABLED_AT, disabledAt)
        .set(USERS.UPDATED_AT, now)
        .where(USERS.ID.eq(userId))
        .execute();
  }

  public long countAll() {
    Long result =
        dsl.selectCount().from(USERS).where(USERS.DELETED_AT.isNull()).fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }

  public long countDisabled() {
    Long result =
        dsl.selectCount()
            .from(USERS)
            .where(USERS.DELETED_AT.isNull().and(USERS.DISABLED_AT.isNotNull()))
            .fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }
}
