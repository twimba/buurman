package com.buurman.repository;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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
                    USERS.FIRST_NAME.lower().like(searchPattern)
                            .or(USERS.LAST_NAME.lower().like(searchPattern))
                            .or(AUDIT_LOG.ENTITY_TYPE.lower().like(searchPattern))
                            .or(AUDIT_LOG.ACTION.lower().like(searchPattern))
                            // Property search (address: street, city, postal code, identifier)
                            .or(PROPERTIES.IDENTIFIER.lower().like(searchPattern))
                            .or(PROPERTIES.STREET.lower().like(searchPattern))
                            .or(PROPERTIES.CITY.lower().like(searchPattern))
                            .or(PROPERTIES.POSTAL_CODE.lower().like(searchPattern))
                            .or(PROPERTIES.COUNTRY.lower().like(searchPattern))
                            // Tenant search (name, email, phone, identifier)
                            .or(TENANTS.IDENTIFIER.lower().like(searchPattern))
                            .or(TENANTS.FIRST_NAME.lower().like(searchPattern))
                            .or(TENANTS.LAST_NAME.lower().like(searchPattern))
                            .or(TENANTS.EMAIL.lower().like(searchPattern))
                            .or(TENANTS.PHONE.lower().like(searchPattern))
                            // Contract search
                            .or(CONTRACTS.IDENTIFIER.lower().like(searchPattern))
                            // Payment search
                            .or(PAYMENTS.IDENTIFIER.lower().like(searchPattern))
                            // Expense search
                            .or(EXPENSES.IDENTIFIER.lower().like(searchPattern))
                            .or(EXPENSES.DESCRIPTION.lower().like(searchPattern))
            );
        }

        return query.orderBy(AUDIT_LOG.TIMESTAMP.desc())
                .fetch()
                .stream().map(r -> (Record) r).toList();
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
