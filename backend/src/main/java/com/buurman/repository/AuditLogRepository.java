package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.AUDIT_LOG;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.jooq.generated.Tables.TENANTS;
import static com.buurman.jooq.generated.Tables.USERS;
import static org.jooq.impl.DSL.lower;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.SortField;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.AuditLogEntry;
import com.buurman.domain.Sid;
import com.buurman.dto.request.PageRequest;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuditLogRepository {

  private final DSLContext dsl;

  public void insertAuditLog(
      UUID id,
      UUID teamId,
      String entityType,
      UUID entityId,
      String action,
      @Nullable JSONB changedFields,
      @Nullable JSONB oldValues,
      @Nullable JSONB newValues,
      UUID userId,
      LocalDateTime timestamp) {
    dsl.insertInto(AUDIT_LOG)
        .set(AUDIT_LOG.ID, id)
        .set(AUDIT_LOG.TEAM_ID, teamId)
        .set(AUDIT_LOG.ENTITY_TYPE, entityType)
        .set(AUDIT_LOG.ENTITY_ID, entityId)
        .set(AUDIT_LOG.ACTION, action)
        .set(AUDIT_LOG.CHANGED_FIELDS, changedFields)
        .set(AUDIT_LOG.OLD_VALUES, oldValues)
        .set(AUDIT_LOG.NEW_VALUES, newValues)
        .set(AUDIT_LOG.USER_ID, userId)
        .set(AUDIT_LOG.TIMESTAMP, timestamp)
        .execute();
  }

  public List<AuditLogEntry> findByTeamIdAndEntityTypeAndEntityId(
      UUID teamId, String entityType, UUID entityId) {
    return dsl.select(
            AUDIT_LOG.ID,
            AUDIT_LOG.ENTITY_TYPE,
            AUDIT_LOG.ENTITY_ID,
            AUDIT_LOG.ACTION,
            AUDIT_LOG.TIMESTAMP,
            AUDIT_LOG.CHANGED_FIELDS,
            AUDIT_LOG.OLD_VALUES,
            AUDIT_LOG.NEW_VALUES,
            USERS.FIRST_NAME,
            USERS.LAST_NAME)
        .from(AUDIT_LOG)
        .leftJoin(USERS)
        .on(AUDIT_LOG.USER_ID.eq(USERS.ID))
        .where(
            AUDIT_LOG
                .TEAM_ID
                .eq(teamId)
                .and(AUDIT_LOG.ENTITY_TYPE.eq(entityType))
                .and(AUDIT_LOG.ENTITY_ID.eq(entityId)))
        .orderBy(AUDIT_LOG.TIMESTAMP.desc())
        .fetch()
        .map(
            r ->
                new AuditLogEntry(
                    r.get(AUDIT_LOG.ID),
                    r.get(AUDIT_LOG.ENTITY_TYPE),
                    r.get(AUDIT_LOG.ENTITY_ID),
                    r.get(AUDIT_LOG.ACTION),
                    r.get(AUDIT_LOG.TIMESTAMP),
                    Optional.ofNullable(r.get(AUDIT_LOG.CHANGED_FIELDS)).map(j -> j.data()),
                    Optional.ofNullable(r.get(AUDIT_LOG.OLD_VALUES)).map(j -> j.data()),
                    Optional.ofNullable(r.get(AUDIT_LOG.NEW_VALUES)).map(j -> j.data()),
                    Optional.ofNullable(r.get(USERS.FIRST_NAME)),
                    Optional.ofNullable(r.get(USERS.LAST_NAME))));
  }

  public List<AuditLogEntry> findAllByTeamId(
      UUID teamId, @Nullable String entityType, @Nullable String action, @Nullable String search) {
    var query =
        dsl.select(
                AUDIT_LOG.ID,
                AUDIT_LOG.ENTITY_TYPE,
                AUDIT_LOG.ENTITY_ID,
                AUDIT_LOG.ACTION,
                AUDIT_LOG.TIMESTAMP,
                AUDIT_LOG.CHANGED_FIELDS,
                AUDIT_LOG.OLD_VALUES,
                AUDIT_LOG.NEW_VALUES,
                USERS.FIRST_NAME,
                USERS.LAST_NAME)
            .from(AUDIT_LOG)
            .leftJoin(USERS)
            .on(AUDIT_LOG.USER_ID.eq(USERS.ID))
            .leftJoin(PROPERTIES)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("PROPERTY").and(AUDIT_LOG.ENTITY_ID.eq(PROPERTIES.ID)))
            .leftJoin(TENANTS)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("TENANT").and(AUDIT_LOG.ENTITY_ID.eq(TENANTS.ID)))
            .leftJoin(CONTRACTS)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("CONTRACT").and(AUDIT_LOG.ENTITY_ID.eq(CONTRACTS.ID)))
            .leftJoin(PAYMENTS)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("PAYMENT").and(AUDIT_LOG.ENTITY_ID.eq(PAYMENTS.ID)))
            .leftJoin(EXPENSES)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("EXPENSE").and(AUDIT_LOG.ENTITY_ID.eq(EXPENSES.ID)))
            .where(AUDIT_LOG.TEAM_ID.eq(teamId));

    if (entityType != null && !entityType.isEmpty()) {
      query = query.and(AUDIT_LOG.ENTITY_TYPE.eq(entityType));
    }

    if (action != null && !action.isEmpty()) {
      query = query.and(AUDIT_LOG.ACTION.eq(action));
    }

    if (search != null && !search.isEmpty()) {
      String searchPattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
      query =
          query.and(
              lower(USERS.FIRST_NAME)
                  .like(searchPattern)
                  .or(lower(USERS.LAST_NAME).like(searchPattern))
                  .or(lower(AUDIT_LOG.ENTITY_TYPE).like(searchPattern))
                  .or(lower(AUDIT_LOG.ACTION).like(searchPattern))
                  // Property search (address: street, city, postal code, identifier)
                  .or(lower(PROPERTIES.IDENTIFIER.cast(String.class)).like(searchPattern))
                  .or(lower(PROPERTIES.STREET).like(searchPattern))
                  .or(lower(PROPERTIES.CITY).like(searchPattern))
                  .or(lower(PROPERTIES.POSTAL_CODE).like(searchPattern))
                  .or(lower(PROPERTIES.COUNTRY_CODE).like(searchPattern))
                  // Tenant search (name, email, phone, identifier)
                  .or(lower(TENANTS.IDENTIFIER.cast(String.class)).like(searchPattern))
                  .or(lower(TENANTS.FIRST_NAME).like(searchPattern))
                  .or(lower(TENANTS.LAST_NAME).like(searchPattern))
                  .or(lower(TENANTS.EMAIL).like(searchPattern))
                  .or(lower(TENANTS.PHONE).like(searchPattern))
                  // Contract search
                  .or(lower(CONTRACTS.IDENTIFIER.cast(String.class)).like(searchPattern))
                  // Payment search
                  .or(lower(PAYMENTS.IDENTIFIER.cast(String.class)).like(searchPattern))
                  // Expense search
                  .or(lower(EXPENSES.IDENTIFIER.cast(String.class)).like(searchPattern))
                  .or(lower(EXPENSES.DESCRIPTION).like(searchPattern)));
    }

    return query
        .orderBy(AUDIT_LOG.TIMESTAMP.desc())
        .fetch()
        .map(
            r ->
                new AuditLogEntry(
                    r.get(AUDIT_LOG.ID),
                    r.get(AUDIT_LOG.ENTITY_TYPE),
                    r.get(AUDIT_LOG.ENTITY_ID),
                    r.get(AUDIT_LOG.ACTION),
                    r.get(AUDIT_LOG.TIMESTAMP),
                    Optional.ofNullable(r.get(AUDIT_LOG.CHANGED_FIELDS)).map(j -> j.data()),
                    Optional.ofNullable(r.get(AUDIT_LOG.OLD_VALUES)).map(j -> j.data()),
                    Optional.ofNullable(r.get(AUDIT_LOG.NEW_VALUES)).map(j -> j.data()),
                    Optional.ofNullable(r.get(USERS.FIRST_NAME)),
                    Optional.ofNullable(r.get(USERS.LAST_NAME))));
  }

  public PaginatedResult<AuditLogEntry> findAllByTeamIdPaginated(
      UUID teamId,
      @Nullable String entityType,
      @Nullable String action,
      @Nullable String search,
      PageRequest pageRequest) {
    Condition condition = AUDIT_LOG.TEAM_ID.eq(teamId);

    if (entityType != null && !entityType.isEmpty()) {
      condition = condition.and(AUDIT_LOG.ENTITY_TYPE.eq(entityType));
    }
    if (action != null && !action.isEmpty()) {
      condition = condition.and(AUDIT_LOG.ACTION.eq(action));
    }
    if (search != null && !search.isEmpty()) {
      String searchPattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
      condition =
          condition.and(
              lower(USERS.FIRST_NAME)
                  .like(searchPattern)
                  .or(lower(USERS.LAST_NAME).like(searchPattern))
                  .or(lower(AUDIT_LOG.ENTITY_TYPE).like(searchPattern))
                  .or(lower(AUDIT_LOG.ACTION).like(searchPattern))
                  .or(lower(PROPERTIES.IDENTIFIER.cast(String.class)).like(searchPattern))
                  .or(lower(PROPERTIES.STREET).like(searchPattern))
                  .or(lower(PROPERTIES.CITY).like(searchPattern))
                  .or(lower(TENANTS.IDENTIFIER.cast(String.class)).like(searchPattern))
                  .or(lower(TENANTS.FIRST_NAME).like(searchPattern))
                  .or(lower(TENANTS.LAST_NAME).like(searchPattern))
                  .or(lower(CONTRACTS.IDENTIFIER.cast(String.class)).like(searchPattern))
                  .or(lower(PAYMENTS.IDENTIFIER.cast(String.class)).like(searchPattern))
                  .or(lower(EXPENSES.IDENTIFIER.cast(String.class)).like(searchPattern))
                  .or(lower(EXPENSES.DESCRIPTION).like(searchPattern)));
    }

    Map<String, Field<?>> sortableFields =
        Map.of(
            "timestamp", AUDIT_LOG.TIMESTAMP,
            "entityType", AUDIT_LOG.ENTITY_TYPE,
            "action", AUDIT_LOG.ACTION);

    Field<?> sortField =
        pageRequest
            .sort()
            .filter(sortableFields::containsKey)
            .map(sortableFields::get)
            .orElse(AUDIT_LOG.TIMESTAMP);

    SortField<?> orderBy =
        pageRequest.direction().orElse(com.buurman.domain.SortDirection.DESC)
                == com.buurman.domain.SortDirection.ASC
            ? sortField.asc()
            : sortField.desc();

    Long totalCount =
        dsl.selectCount()
            .from(AUDIT_LOG)
            .leftJoin(USERS)
            .on(AUDIT_LOG.USER_ID.eq(USERS.ID))
            .leftJoin(PROPERTIES)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("PROPERTY").and(AUDIT_LOG.ENTITY_ID.eq(PROPERTIES.ID)))
            .leftJoin(TENANTS)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("TENANT").and(AUDIT_LOG.ENTITY_ID.eq(TENANTS.ID)))
            .leftJoin(CONTRACTS)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("CONTRACT").and(AUDIT_LOG.ENTITY_ID.eq(CONTRACTS.ID)))
            .leftJoin(PAYMENTS)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("PAYMENT").and(AUDIT_LOG.ENTITY_ID.eq(PAYMENTS.ID)))
            .leftJoin(EXPENSES)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("EXPENSE").and(AUDIT_LOG.ENTITY_ID.eq(EXPENSES.ID)))
            .where(condition)
            .fetchOne(0, Long.class);
    long totalElements = totalCount != null ? totalCount : 0L;

    List<AuditLogEntry> items =
        dsl.select(
                AUDIT_LOG.ID,
                AUDIT_LOG.ENTITY_TYPE,
                AUDIT_LOG.ENTITY_ID,
                AUDIT_LOG.ACTION,
                AUDIT_LOG.TIMESTAMP,
                AUDIT_LOG.CHANGED_FIELDS,
                AUDIT_LOG.OLD_VALUES,
                AUDIT_LOG.NEW_VALUES,
                USERS.FIRST_NAME,
                USERS.LAST_NAME)
            .from(AUDIT_LOG)
            .leftJoin(USERS)
            .on(AUDIT_LOG.USER_ID.eq(USERS.ID))
            .leftJoin(PROPERTIES)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("PROPERTY").and(AUDIT_LOG.ENTITY_ID.eq(PROPERTIES.ID)))
            .leftJoin(TENANTS)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("TENANT").and(AUDIT_LOG.ENTITY_ID.eq(TENANTS.ID)))
            .leftJoin(CONTRACTS)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("CONTRACT").and(AUDIT_LOG.ENTITY_ID.eq(CONTRACTS.ID)))
            .leftJoin(PAYMENTS)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("PAYMENT").and(AUDIT_LOG.ENTITY_ID.eq(PAYMENTS.ID)))
            .leftJoin(EXPENSES)
            .on(AUDIT_LOG.ENTITY_TYPE.eq("EXPENSE").and(AUDIT_LOG.ENTITY_ID.eq(EXPENSES.ID)))
            .where(condition)
            .orderBy(orderBy)
            .limit(pageRequest.size())
            .offset(pageRequest.offset())
            .fetch()
            .map(
                r ->
                    new AuditLogEntry(
                        r.get(AUDIT_LOG.ID),
                        r.get(AUDIT_LOG.ENTITY_TYPE),
                        r.get(AUDIT_LOG.ENTITY_ID),
                        r.get(AUDIT_LOG.ACTION),
                        r.get(AUDIT_LOG.TIMESTAMP),
                        Optional.ofNullable(r.get(AUDIT_LOG.CHANGED_FIELDS)).map(j -> j.data()),
                        Optional.ofNullable(r.get(AUDIT_LOG.OLD_VALUES)).map(j -> j.data()),
                        Optional.ofNullable(r.get(AUDIT_LOG.NEW_VALUES)).map(j -> j.data()),
                        Optional.ofNullable(r.get(USERS.FIRST_NAME)),
                        Optional.ofNullable(r.get(USERS.LAST_NAME))));

    return new PaginatedResult<>(items, totalElements);
  }

  public List<AuditLogEntry> findRecentByTeamId(UUID teamId, int limit) {
    return dsl.select(
            AUDIT_LOG.ID,
            AUDIT_LOG.ENTITY_TYPE,
            AUDIT_LOG.ENTITY_ID,
            AUDIT_LOG.ACTION,
            AUDIT_LOG.TIMESTAMP,
            USERS.FIRST_NAME,
            USERS.LAST_NAME)
        .from(AUDIT_LOG)
        .leftJoin(USERS)
        .on(AUDIT_LOG.USER_ID.eq(USERS.ID))
        .where(AUDIT_LOG.TEAM_ID.eq(teamId))
        .orderBy(AUDIT_LOG.TIMESTAMP.desc())
        .limit(limit)
        .fetch()
        .map(
            r ->
                new AuditLogEntry(
                    r.get(AUDIT_LOG.ID),
                    r.get(AUDIT_LOG.ENTITY_TYPE),
                    r.get(AUDIT_LOG.ENTITY_ID),
                    r.get(AUDIT_LOG.ACTION),
                    r.get(AUDIT_LOG.TIMESTAMP),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.ofNullable(r.get(USERS.FIRST_NAME)),
                    Optional.ofNullable(r.get(USERS.LAST_NAME))));
  }

  public Optional<Sid> findEntityIdentifier(String entityType, UUID entityId, UUID teamId) {
    return switch (entityType.toLowerCase(Locale.ROOT)) {
      case "property" ->
          dsl.select(PROPERTIES.IDENTIFIER)
              .from(PROPERTIES)
              .where(PROPERTIES.ID.eq(entityId).and(PROPERTIES.TEAM_ID.eq(teamId)))
              .fetchOptional()
              .map(r -> r.get(PROPERTIES.IDENTIFIER));
      case "tenant" ->
          dsl.select(TENANTS.IDENTIFIER)
              .from(TENANTS)
              .where(TENANTS.ID.eq(entityId).and(TENANTS.TEAM_ID.eq(teamId)))
              .fetchOptional()
              .map(r -> r.get(TENANTS.IDENTIFIER));
      case "contract" ->
          dsl.select(CONTRACTS.IDENTIFIER)
              .from(CONTRACTS)
              .where(CONTRACTS.ID.eq(entityId).and(CONTRACTS.TEAM_ID.eq(teamId)))
              .fetchOptional()
              .map(r -> r.get(CONTRACTS.IDENTIFIER));
      case "expense" ->
          dsl.select(EXPENSES.IDENTIFIER)
              .from(EXPENSES)
              .where(EXPENSES.ID.eq(entityId).and(EXPENSES.TEAM_ID.eq(teamId)))
              .fetchOptional()
              .map(r -> r.get(EXPENSES.IDENTIFIER));
      case "payment" ->
          dsl.select(PAYMENTS.IDENTIFIER)
              .from(PAYMENTS)
              .where(PAYMENTS.ID.eq(entityId).and(PAYMENTS.TEAM_ID.eq(teamId)))
              .fetchOptional()
              .map(r -> r.get(PAYMENTS.IDENTIFIER));
      case "team" ->
          dsl.select(TEAMS.IDENTIFIER)
              .from(TEAMS)
              .where(TEAMS.ID.eq(entityId))
              .fetchOptional()
              .map(r -> r.get(TEAMS.IDENTIFIER));
      case "user" ->
          dsl.select(USERS.IDENTIFIER)
              .from(USERS)
              .where(USERS.ID.eq(entityId))
              .fetchOptional()
              .map(r -> r.get(USERS.IDENTIFIER));
      default -> Optional.empty();
    };
  }

  public Optional<String> findEntityName(String entityType, UUID entityId, UUID teamId) {
    return switch (entityType.toLowerCase(Locale.ROOT)) {
      case "property" ->
          dsl.select(PROPERTIES.STREET, PROPERTIES.CITY)
              .from(PROPERTIES)
              .where(PROPERTIES.ID.eq(entityId).and(PROPERTIES.TEAM_ID.eq(teamId)))
              .fetchOptional()
              .map(r -> r.get(PROPERTIES.STREET) + ", " + r.get(PROPERTIES.CITY));
      case "tenant" ->
          dsl.select(TENANTS.FIRST_NAME, TENANTS.LAST_NAME)
              .from(TENANTS)
              .where(TENANTS.ID.eq(entityId).and(TENANTS.TEAM_ID.eq(teamId)))
              .fetchOptional()
              .map(
                  r -> {
                    String firstName = r.get(TENANTS.FIRST_NAME);
                    String lastName = r.get(TENANTS.LAST_NAME);
                    return lastName != null ? firstName + " " + lastName : firstName;
                  });
      case "team" ->
          dsl.select(TEAMS.NAME)
              .from(TEAMS)
              .where(TEAMS.ID.eq(entityId))
              .fetchOptional()
              .map(r -> r.get(TEAMS.NAME));
      case "user" ->
          dsl.select(USERS.FIRST_NAME, USERS.LAST_NAME)
              .from(USERS)
              .where(USERS.ID.eq(entityId))
              .fetchOptional()
              .map(r -> r.get(USERS.FIRST_NAME) + " " + r.get(USERS.LAST_NAME));
      default -> Optional.empty();
    };
  }

  public void deleteByEntityAndTeamId(String entityType, UUID entityId, UUID teamId) {
    dsl.deleteFrom(AUDIT_LOG)
        .where(
            AUDIT_LOG
                .ENTITY_TYPE
                .equalIgnoreCase(entityType)
                .and(AUDIT_LOG.ENTITY_ID.eq(entityId))
                .and(AUDIT_LOG.TEAM_ID.eq(teamId)))
        .execute();
  }
}
