package com.buurman.service;

import com.buurman.domain.AuditLog;
import com.buurman.dto.response.RecentActivityResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.AUDIT_LOG;
import static com.buurman.jooq.generated.Tables.USERS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.TENANTS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public AuditService(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    public void logCreate(UUID teamId, String entityType, UUID entityId, UUID userId, Object entity) {
        try {
            Map<String, Object> newValues = objectToMap(entity);

            dsl.insertInto(AUDIT_LOG)
                    .set(AUDIT_LOG.ID, UUID.randomUUID())
                    .set(AUDIT_LOG.TEAM_ID, teamId)
                    .set(AUDIT_LOG.ENTITY_TYPE, entityType)
                    .set(AUDIT_LOG.ENTITY_ID, entityId)
                    .set(AUDIT_LOG.ACTION, AuditLog.Action.CREATE.name())
                    .set(AUDIT_LOG.NEW_VALUES, JSONB.valueOf(objectMapper.writeValueAsString(newValues)))
                    .set(AUDIT_LOG.USER_ID, userId)
                    .set(AUDIT_LOG.TIMESTAMP, LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                    .execute();

            log.debug("Audit log created: {} {} for team {}", AuditLog.Action.CREATE, entityType, teamId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize entity for audit log", e);
        }
    }

    public void logUpdate(UUID teamId, String entityType, UUID entityId, UUID userId,
                          Object oldEntity, Object newEntity, Map<String, Object> changedFields) {
        try {
            Map<String, Object> oldValues = objectToMap(oldEntity);
            Map<String, Object> newValues = objectToMap(newEntity);

            dsl.insertInto(AUDIT_LOG)
                    .set(AUDIT_LOG.ID, UUID.randomUUID())
                    .set(AUDIT_LOG.TEAM_ID, teamId)
                    .set(AUDIT_LOG.ENTITY_TYPE, entityType)
                    .set(AUDIT_LOG.ENTITY_ID, entityId)
                    .set(AUDIT_LOG.ACTION, AuditLog.Action.UPDATE.name())
                    .set(AUDIT_LOG.CHANGED_FIELDS, JSONB.valueOf(objectMapper.writeValueAsString(changedFields)))
                    .set(AUDIT_LOG.OLD_VALUES, JSONB.valueOf(objectMapper.writeValueAsString(oldValues)))
                    .set(AUDIT_LOG.NEW_VALUES, JSONB.valueOf(objectMapper.writeValueAsString(newValues)))
                    .set(AUDIT_LOG.USER_ID, userId)
                    .set(AUDIT_LOG.TIMESTAMP, LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                    .execute();

            log.debug("Audit log created: {} {} for team {}", AuditLog.Action.UPDATE, entityType, teamId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize entity for audit log", e);
        }
    }

    public void logDelete(UUID teamId, String entityType, UUID entityId, UUID userId, Object entity) {
        try {
            Map<String, Object> oldValues = objectToMap(entity);

            dsl.insertInto(AUDIT_LOG)
                    .set(AUDIT_LOG.ID, UUID.randomUUID())
                    .set(AUDIT_LOG.TEAM_ID, teamId)
                    .set(AUDIT_LOG.ENTITY_TYPE, entityType)
                    .set(AUDIT_LOG.ENTITY_ID, entityId)
                    .set(AUDIT_LOG.ACTION, AuditLog.Action.DELETE.name())
                    .set(AUDIT_LOG.OLD_VALUES, JSONB.valueOf(objectMapper.writeValueAsString(oldValues)))
                    .set(AUDIT_LOG.USER_ID, userId)
                    .set(AUDIT_LOG.TIMESTAMP, LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                    .execute();

            log.debug("Audit log created: {} {} for team {}", AuditLog.Action.DELETE, entityType, teamId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize entity for audit log", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectToMap(Object obj) {
        return objectMapper.convertValue(obj, Map.class);
    }

    public Map<String, Object> getChangedFields(Object oldEntity, Object newEntity) {
        Map<String, Object> oldMap = objectToMap(oldEntity);
        Map<String, Object> newMap = objectToMap(newEntity);

        Map<String, Object> changes = new HashMap<>();
        for (String key : newMap.keySet()) {
            Object oldValue = oldMap.get(key);
            Object newValue = newMap.get(key);

            if (oldValue == null && newValue != null) {
                changes.put(key, newValue);
            } else if (oldValue != null && !oldValue.equals(newValue)) {
                changes.put(key, newValue);
            }
        }

        return changes;
    }

    public List<RecentActivityResponse> getEntityAuditLog(UUID teamId, String entityType, UUID entityId) {
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
                .map(record -> mapRecordToRecentActivity(record, entityType));
    }

    public List<RecentActivityResponse> getAllAuditLogs(UUID teamId, String entityType, String action, String search) {
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

        // Apply filters
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
                .map(record -> mapRecordToRecentActivity(
                        record,
                        record.get(AUDIT_LOG.ENTITY_TYPE)
                ));
    }

    private RecentActivityResponse mapRecordToRecentActivity(org.jooq.Record record, String entityType) {
        String action = record.get(AUDIT_LOG.ACTION);
        String firstName = record.get(USERS.FIRST_NAME);
        String lastName = record.get(USERS.LAST_NAME);
        String userName = (firstName != null && lastName != null)
                ? firstName + " " + lastName
                : "Unknown";

        // Parse JSON fields first
        Map<String, Object> changedFields = parseJsonbField(record.get(AUDIT_LOG.CHANGED_FIELDS));
        Map<String, Object> oldValues = parseJsonbField(record.get(AUDIT_LOG.OLD_VALUES));
        Map<String, Object> newValues = parseJsonbField(record.get(AUDIT_LOG.NEW_VALUES));

        // Build description based on action and changed fields
        String description = buildActivityDescription(action, entityType, userName, changedFields);

        return new RecentActivityResponse(
                record.get(AUDIT_LOG.ID),
                entityType,
                record.get(AUDIT_LOG.ENTITY_ID),
                entityType, // entityName - can be enhanced later
                action,
                userName,
                record.get(AUDIT_LOG.TIMESTAMP).toInstant(ZoneOffset.UTC),
                description,
                changedFields,
                oldValues,
                newValues
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonbField(JSONB jsonb) {
        if (jsonb == null || jsonb.data() == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(jsonb.data(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSONB field", e);
            return Map.of();
        }
    }

    private String buildActivityDescription(String action, String entityType, String userName, Map<String, Object> changedFields) {
        // Check for document operations
        if (changedFields != null && changedFields.containsKey("documentAdded")) {
            String fileName = (String) changedFields.get("documentAdded");
            String category = (String) changedFields.get("category");
            String docType = "PHOTO".equals(category) ? "photo" : "document";
            return String.format("%s uploaded %s: %s", userName, docType, fileName);
        }

        if (changedFields != null && changedFields.containsKey("documentRemoved")) {
            String fileName = (String) changedFields.get("documentRemoved");
            String category = (String) changedFields.get("category");
            String docType = "PHOTO".equals(category) ? "photo" : "document";
            return String.format("%s removed %s: %s", userName, docType, fileName);
        }

        // Default behavior for other operations
        String actionText = switch (action) {
            case "CREATE" -> "created";
            case "UPDATE" -> "updated";
            case "DELETE" -> "deleted";
            case "RESTORE" -> "restored";
            default -> "modified";
        };

        return String.format("%s %s this %s", userName, actionText, entityType.toLowerCase());
    }
}
