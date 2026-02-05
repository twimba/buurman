package com.buurman.service;

import com.buurman.domain.AuditLog;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.repository.AuditLogRepository;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSONB;
import org.jooq.Record;
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

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper,
                        MetricsService metricsService) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    public void logCreate(UUID teamId, String entityType, UUID entityId, UUID userId, Object entity) {
        try {
            Map<String, Object> newValues = objectToMap(entity);

            auditLogRepository.insertAuditLog(
                    UUID.randomUUID(),
                    teamId,
                    entityType,
                    entityId,
                    AuditLog.Action.CREATE.name(),
                    null,
                    null,
                    JSONB.valueOf(objectMapper.writeValueAsString(newValues)),
                    userId,
                    LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            );

            metricsService.incrementCounter("audit.log.total",
                    "entity_type", entityType, "action", "CREATE");
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

            auditLogRepository.insertAuditLog(
                    UUID.randomUUID(),
                    teamId,
                    entityType,
                    entityId,
                    AuditLog.Action.UPDATE.name(),
                    JSONB.valueOf(objectMapper.writeValueAsString(changedFields)),
                    JSONB.valueOf(objectMapper.writeValueAsString(oldValues)),
                    JSONB.valueOf(objectMapper.writeValueAsString(newValues)),
                    userId,
                    LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            );

            metricsService.incrementCounter("audit.log.total",
                    "entity_type", entityType, "action", "UPDATE");
            log.debug("Audit log created: {} {} for team {}", AuditLog.Action.UPDATE, entityType, teamId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize entity for audit log", e);
        }
    }

    public void logDelete(UUID teamId, String entityType, UUID entityId, UUID userId, Object entity) {
        try {
            Map<String, Object> oldValues = objectToMap(entity);

            auditLogRepository.insertAuditLog(
                    UUID.randomUUID(),
                    teamId,
                    entityType,
                    entityId,
                    AuditLog.Action.DELETE.name(),
                    null,
                    JSONB.valueOf(objectMapper.writeValueAsString(oldValues)),
                    null,
                    userId,
                    LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            );

            metricsService.incrementCounter("audit.log.total",
                    "entity_type", entityType, "action", "DELETE");
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
        return auditLogRepository.findByTeamIdAndEntityTypeAndEntityId(teamId, entityType, entityId)
                .stream()
                .map(record -> mapRecordToRecentActivity(record, entityType, teamId))
                .toList();
    }

    public PageResponse<RecentActivityResponse> getAllAuditLogsPaginated(UUID teamId, String entityType, String action, String search, PageRequest pageRequest) {
        PaginatedResult<Record> result = auditLogRepository.findAllByTeamIdPaginated(
                teamId, entityType, action, search, pageRequest);
        List<RecentActivityResponse> responses = result.items().stream()
                .map(record -> mapRecordToRecentActivity(record, record.get(AUDIT_LOG.ENTITY_TYPE), teamId))
                .toList();
        return PageResponse.of(responses, pageRequest.page(), pageRequest.size(), result.totalElements());
    }

    public List<RecentActivityResponse> getAllAuditLogs(UUID teamId, String entityType, String action, String search) {
        return auditLogRepository.findAllByTeamId(teamId, entityType, action, search)
                .stream()
                .map(record -> mapRecordToRecentActivity(record, record.get(AUDIT_LOG.ENTITY_TYPE), teamId))
                .toList();
    }

    private RecentActivityResponse mapRecordToRecentActivity(Record record, String entityType, UUID teamId) {
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

        // Resolve entity identifier from entity UUID
        UUID entityId = record.get(AUDIT_LOG.ENTITY_ID);
        String entityIdentifier = auditLogRepository.findEntityIdentifier(entityType, entityId, teamId)
                .orElse(entityId != null ? entityId.toString() : "unknown");

        return new RecentActivityResponse(
                entityType,
                entityIdentifier,
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

        // Check for photo operations
        if (changedFields != null && changedFields.containsKey("photoAdded")) {
            String fileName = (String) changedFields.get("photoAdded");
            return String.format("%s uploaded photo: %s", userName, fileName);
        }

        if (changedFields != null && changedFields.containsKey("photoRemoved")) {
            String fileName = (String) changedFields.get("photoRemoved");
            return String.format("%s removed photo: %s", userName, fileName);
        }

        if (changedFields != null && changedFields.containsKey("photoEdited")) {
            String fileName = (String) changedFields.get("photoEdited");
            return String.format("%s edited photo metadata: %s", userName, fileName);
        }

        if (changedFields != null && changedFields.containsKey("documentEdited")) {
            String fileName = (String) changedFields.get("documentEdited");
            return String.format("%s edited document metadata: %s", userName, fileName);
        }

        // Check for receival operations
        if (changedFields != null && changedFields.containsKey("receivalRegistered")) {
            return String.format("%s registered a receival: %s", userName, changedFields.get("receivalRegistered"));
        }

        if (changedFields != null && changedFields.containsKey("receivalUpdated")) {
            return String.format("%s updated a receival: %s", userName, changedFields.get("receivalUpdated"));
        }

        if (changedFields != null && changedFields.containsKey("receivalDeleted")) {
            return String.format("%s deleted a receival: %s", userName, changedFields.get("receivalDeleted"));
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
