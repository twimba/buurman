package com.buurman.repository;

import com.buurman.dto.request.PageRequest;
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import static org.jooq.impl.DSL.lower;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.*;

@Repository
public class AuditLogRepository {

    private final DSLContext dsl;

    public AuditLogRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void insertAuditLog(UUID id, UUID teamId, String entityType, UUID entityId,
                               String action, JSONB changedFields, JSONB oldValues,
                               JSONB newValues, UUID userId, LocalDateTime timestamp) {
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

    public List<Record> findByTeamIdAndEntityTypeAndEntityId(UUID teamId, String entityType, UUID entityId) {
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
                        USERS.LAST_NAME
                )
                .from(AUDIT_LOG)
                .leftJoin(USERS).on(AUDIT_LOG.USER_ID.eq(USERS.ID))
                .where(AUDIT_LOG.TEAM_ID.eq(teamId)
                        .and(AUDIT_LOG.ENTITY_TYPE.eq(entityType))
                        .and(AUDIT_LOG.ENTITY_ID.eq(entityId)))
                .orderBy(AUDIT_LOG.TIMESTAMP.desc())
                .fetch()
                .stream().map(r -> (Record) r).toList();
    }

    public List<Record> findAllByTeamId(UUID teamId, String entityType, String action, String search) {
        var query = dsl.select(
                        AUDIT_LOG.ID,
                        AUDIT_LOG.ENTITY_TYPE,
                        AUDIT_LOG.ENTITY_ID,
                        AUDIT_LOG.ACTION,
                        AUDIT_LOG.TIMESTAMP,
                        AUDIT_LOG.CHANGED_FIELDS,
                        AUDIT_LOG.OLD_VALUES,
                        AUDIT_LOG.NEW_VALUES,
                        USERS.FIRST_NAME,
                        USERS.LAST_NAME
                )
                .from(AUDIT_LOG)
                .leftJoin(USERS).on(AUDIT_LOG.USER_ID.eq(USERS.ID))
                .leftJoin(PROPERTIES).on(
                        AUDIT_LOG.ENTITY_TYPE.eq("PROPERTY")
                                .and(AUDIT_LOG.ENTITY_ID.eq(PROPERTIES.ID))
                )
                .leftJoin(TENANTS).on(
                        AUDIT_LOG.ENTITY_TYPE.eq("TENANT")
                                .and(AUDIT_LOG.ENTITY_ID.eq(TENANTS.ID))
                )
                .leftJoin(CONTRACTS).on(
                        AUDIT_LOG.ENTITY_TYPE.eq("CONTRACT")
                                .and(AUDIT_LOG.ENTITY_ID.eq(CONTRACTS.ID))
                )
                .leftJoin(PAYMENTS).on(
                        AUDIT_LOG.ENTITY_TYPE.eq("PAYMENT")
                                .and(AUDIT_LOG.ENTITY_ID.eq(PAYMENTS.ID))
                )
                .leftJoin(EXPENSES).on(
                        AUDIT_LOG.ENTITY_TYPE.eq("EXPENSE")
                                .and(AUDIT_LOG.ENTITY_ID.eq(EXPENSES.ID))
                )
                .where(AUDIT_LOG.TEAM_ID.eq(teamId));

        if (entityType != null && !entityType.isEmpty()) {
            query = query.and(AUDIT_LOG.ENTITY_TYPE.eq(entityType));
        }

        if (action != null && !action.isEmpty()) {
            query = query.and(AUDIT_LOG.ACTION.eq(action));
        }

        if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            query = query.and(
                    lower(USERS.FIRST_NAME).like(searchPattern)
                            .or(lower(USERS.LAST_NAME).like(searchPattern))
                            .or(lower(AUDIT_LOG.ENTITY_TYPE).like(searchPattern))
                            .or(lower(AUDIT_LOG.ACTION).like(searchPattern))
                            // Property search (address: street, city, postal code, identifier)
                            .or(lower(PROPERTIES.IDENTIFIER).like(searchPattern))
                            .or(lower(PROPERTIES.STREET).like(searchPattern))
                            .or(lower(PROPERTIES.CITY).like(searchPattern))
                            .or(lower(PROPERTIES.POSTAL_CODE).like(searchPattern))
                            .or(lower(PROPERTIES.COUNTRY).like(searchPattern))
                            // Tenant search (name, email, phone, identifier)
                            .or(lower(TENANTS.IDENTIFIER).like(searchPattern))
                            .or(lower(TENANTS.FIRST_NAME).like(searchPattern))
                            .or(lower(TENANTS.LAST_NAME).like(searchPattern))
                            .or(lower(TENANTS.EMAIL).like(searchPattern))
                            .or(lower(TENANTS.PHONE).like(searchPattern))
                            // Contract search
                            .or(lower(CONTRACTS.IDENTIFIER).like(searchPattern))
                            // Payment search
                            .or(lower(PAYMENTS.IDENTIFIER).like(searchPattern))
                            // Expense search
                            .or(lower(EXPENSES.IDENTIFIER).like(searchPattern))
                            .or(lower(EXPENSES.DESCRIPTION).like(searchPattern))
            );
        }

        return query.orderBy(AUDIT_LOG.TIMESTAMP.desc())
                .fetch()
                .stream().map(r -> (Record) r).toList();
    }

    public PaginatedResult<Record> findAllByTeamIdPaginated(UUID teamId, String entityType, String action, String search, PageRequest pageRequest) {
        Condition condition = AUDIT_LOG.TEAM_ID.eq(teamId);

        if (entityType != null && !entityType.isEmpty()) {
            condition = condition.and(AUDIT_LOG.ENTITY_TYPE.eq(entityType));
        }
        if (action != null && !action.isEmpty()) {
            condition = condition.and(AUDIT_LOG.ACTION.eq(action));
        }
        if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search.toLowerCase() + "%";
            condition = condition.and(
                lower(USERS.FIRST_NAME).like(searchPattern)
                    .or(lower(USERS.LAST_NAME).like(searchPattern))
                    .or(lower(AUDIT_LOG.ENTITY_TYPE).like(searchPattern))
                    .or(lower(AUDIT_LOG.ACTION).like(searchPattern))
                    .or(lower(PROPERTIES.IDENTIFIER).like(searchPattern))
                    .or(lower(PROPERTIES.STREET).like(searchPattern))
                    .or(lower(PROPERTIES.CITY).like(searchPattern))
                    .or(lower(TENANTS.IDENTIFIER).like(searchPattern))
                    .or(lower(TENANTS.FIRST_NAME).like(searchPattern))
                    .or(lower(TENANTS.LAST_NAME).like(searchPattern))
                    .or(lower(CONTRACTS.IDENTIFIER).like(searchPattern))
                    .or(lower(PAYMENTS.IDENTIFIER).like(searchPattern))
                    .or(lower(EXPENSES.IDENTIFIER).like(searchPattern))
                    .or(lower(EXPENSES.DESCRIPTION).like(searchPattern))
            );
        }

        Map<String, Field<?>> sortableFields = Map.of(
            "timestamp", AUDIT_LOG.TIMESTAMP,
            "entityType", AUDIT_LOG.ENTITY_TYPE,
            "action", AUDIT_LOG.ACTION
        );

        Field<?> sortField = (pageRequest.sort() != null && sortableFields.containsKey(pageRequest.sort()))
                ? sortableFields.get(pageRequest.sort())
                : AUDIT_LOG.TIMESTAMP;

        SortField<?> orderBy = "asc".equals(pageRequest.direction()) ? sortField.asc() : sortField.desc();

        long totalElements = dsl.selectCount()
                .from(AUDIT_LOG)
                .leftJoin(USERS).on(AUDIT_LOG.USER_ID.eq(USERS.ID))
                .leftJoin(PROPERTIES).on(AUDIT_LOG.ENTITY_TYPE.eq("PROPERTY").and(AUDIT_LOG.ENTITY_ID.eq(PROPERTIES.ID)))
                .leftJoin(TENANTS).on(AUDIT_LOG.ENTITY_TYPE.eq("TENANT").and(AUDIT_LOG.ENTITY_ID.eq(TENANTS.ID)))
                .leftJoin(CONTRACTS).on(AUDIT_LOG.ENTITY_TYPE.eq("CONTRACT").and(AUDIT_LOG.ENTITY_ID.eq(CONTRACTS.ID)))
                .leftJoin(PAYMENTS).on(AUDIT_LOG.ENTITY_TYPE.eq("PAYMENT").and(AUDIT_LOG.ENTITY_ID.eq(PAYMENTS.ID)))
                .leftJoin(EXPENSES).on(AUDIT_LOG.ENTITY_TYPE.eq("EXPENSE").and(AUDIT_LOG.ENTITY_ID.eq(EXPENSES.ID)))
                .where(condition)
                .fetchOne(0, long.class);

        List<Record> items = dsl.select(
                        AUDIT_LOG.ID,
                        AUDIT_LOG.ENTITY_TYPE,
                        AUDIT_LOG.ENTITY_ID,
                        AUDIT_LOG.ACTION,
                        AUDIT_LOG.TIMESTAMP,
                        AUDIT_LOG.CHANGED_FIELDS,
                        AUDIT_LOG.OLD_VALUES,
                        AUDIT_LOG.NEW_VALUES,
                        USERS.FIRST_NAME,
                        USERS.LAST_NAME
                )
                .from(AUDIT_LOG)
                .leftJoin(USERS).on(AUDIT_LOG.USER_ID.eq(USERS.ID))
                .leftJoin(PROPERTIES).on(AUDIT_LOG.ENTITY_TYPE.eq("PROPERTY").and(AUDIT_LOG.ENTITY_ID.eq(PROPERTIES.ID)))
                .leftJoin(TENANTS).on(AUDIT_LOG.ENTITY_TYPE.eq("TENANT").and(AUDIT_LOG.ENTITY_ID.eq(TENANTS.ID)))
                .leftJoin(CONTRACTS).on(AUDIT_LOG.ENTITY_TYPE.eq("CONTRACT").and(AUDIT_LOG.ENTITY_ID.eq(CONTRACTS.ID)))
                .leftJoin(PAYMENTS).on(AUDIT_LOG.ENTITY_TYPE.eq("PAYMENT").and(AUDIT_LOG.ENTITY_ID.eq(PAYMENTS.ID)))
                .leftJoin(EXPENSES).on(AUDIT_LOG.ENTITY_TYPE.eq("EXPENSE").and(AUDIT_LOG.ENTITY_ID.eq(EXPENSES.ID)))
                .where(condition)
                .orderBy(orderBy)
                .limit(pageRequest.size())
                .offset(pageRequest.offset())
                .fetch()
                .stream().map(r -> (Record) r).toList();

        return new PaginatedResult<>(items, totalElements);
    }

    public List<Record> findRecentByTeamId(UUID teamId, int limit) {
        return dsl.select(
                        AUDIT_LOG.ID,
                        AUDIT_LOG.ENTITY_TYPE,
                        AUDIT_LOG.ENTITY_ID,
                        AUDIT_LOG.ACTION,
                        AUDIT_LOG.TIMESTAMP,
                        USERS.FIRST_NAME,
                        USERS.LAST_NAME
                )
                .from(AUDIT_LOG)
                .leftJoin(USERS).on(AUDIT_LOG.USER_ID.eq(USERS.ID))
                .where(AUDIT_LOG.TEAM_ID.eq(teamId))
                .orderBy(AUDIT_LOG.TIMESTAMP.desc())
                .limit(limit)
                .fetch()
                .stream().map(r -> (Record) r).toList();
    }

    public Optional<String> findEntityIdentifier(String entityType, UUID entityId, UUID teamId) {
        return switch (entityType.toLowerCase()) {
            case "property" -> dsl.select(PROPERTIES.IDENTIFIER)
                    .from(PROPERTIES)
                    .where(PROPERTIES.ID.eq(entityId)
                            .and(PROPERTIES.TEAM_ID.eq(teamId)))
                    .fetchOptional()
                    .map(r -> r.get(PROPERTIES.IDENTIFIER));
            case "tenant" -> dsl.select(TENANTS.IDENTIFIER)
                    .from(TENANTS)
                    .where(TENANTS.ID.eq(entityId)
                            .and(TENANTS.TEAM_ID.eq(teamId)))
                    .fetchOptional()
                    .map(r -> r.get(TENANTS.IDENTIFIER));
            case "contract" -> dsl.select(CONTRACTS.IDENTIFIER)
                    .from(CONTRACTS)
                    .where(CONTRACTS.ID.eq(entityId)
                            .and(CONTRACTS.TEAM_ID.eq(teamId)))
                    .fetchOptional()
                    .map(r -> r.get(CONTRACTS.IDENTIFIER));
            case "expense" -> dsl.select(EXPENSES.IDENTIFIER)
                    .from(EXPENSES)
                    .where(EXPENSES.ID.eq(entityId)
                            .and(EXPENSES.TEAM_ID.eq(teamId)))
                    .fetchOptional()
                    .map(r -> r.get(EXPENSES.IDENTIFIER));
            case "payment" -> dsl.select(PAYMENTS.IDENTIFIER)
                    .from(PAYMENTS)
                    .where(PAYMENTS.ID.eq(entityId)
                            .and(PAYMENTS.TEAM_ID.eq(teamId)))
                    .fetchOptional()
                    .map(r -> r.get(PAYMENTS.IDENTIFIER));
            case "team" -> dsl.select(TEAMS.IDENTIFIER)
                    .from(TEAMS)
                    .where(TEAMS.ID.eq(entityId))
                    .fetchOptional()
                    .map(r -> r.get(TEAMS.IDENTIFIER));
            case "user" -> dsl.select(USERS.IDENTIFIER)
                    .from(USERS)
                    .where(USERS.ID.eq(entityId))
                    .fetchOptional()
                    .map(r -> r.get(USERS.IDENTIFIER));
            default -> Optional.empty();
        };
    }

    public Optional<String> findEntityName(String entityType, UUID entityId, UUID teamId) {
        return switch (entityType.toLowerCase()) {
            case "property" -> dsl.select(PROPERTIES.STREET, PROPERTIES.CITY)
                    .from(PROPERTIES)
                    .where(PROPERTIES.ID.eq(entityId)
                            .and(PROPERTIES.TEAM_ID.eq(teamId)))
                    .fetchOptional()
                    .map(r -> r.get(PROPERTIES.STREET) + ", " + r.get(PROPERTIES.CITY));
            case "tenant" -> dsl.select(TENANTS.FIRST_NAME, TENANTS.LAST_NAME)
                    .from(TENANTS)
                    .where(TENANTS.ID.eq(entityId)
                            .and(TENANTS.TEAM_ID.eq(teamId)))
                    .fetchOptional()
                    .map(r -> {
                        String firstName = r.get(TENANTS.FIRST_NAME);
                        String lastName = r.get(TENANTS.LAST_NAME);
                        return lastName != null ? firstName + " " + lastName : firstName;
                    });
            case "team" -> dsl.select(TEAMS.NAME)
                    .from(TEAMS)
                    .where(TEAMS.ID.eq(entityId))
                    .fetchOptional()
                    .map(r -> r.get(TEAMS.NAME));
            case "user" -> dsl.select(USERS.FIRST_NAME, USERS.LAST_NAME)
                    .from(USERS)
                    .where(USERS.ID.eq(entityId))
                    .fetchOptional()
                    .map(r -> r.get(USERS.FIRST_NAME) + " " + r.get(USERS.LAST_NAME));
            default -> Optional.empty();
        };
    }
}
