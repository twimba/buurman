package com.buurman.service;

import static com.buurman.domain.AuditLog.Action.CREATE;
import static com.buurman.domain.AuditLog.Action.DELETE;
import static com.buurman.domain.AuditLog.Action.UPDATE;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.JSONB;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.buurman.domain.AuditLogEntry;
import com.buurman.domain.ContractPartyRole;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.repository.AuditLogRepository;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;
  private final MetricsService metricsService;
  private final Clock clock;

  public void logCreate(UUID teamId, String entityType, UUID entityId, UUID userId, Object entity) {
    try {
      Map<String, Object> newValues = objectToMap(entity);

      auditLogRepository.insertAuditLog(
          UUID.randomUUID(),
          teamId,
          entityType,
          entityId,
          CREATE.name(),
          null,
          null,
          JSONB.valueOf(objectMapper.writeValueAsString(newValues)),
          userId,
          LocalDateTime.now(clock));

      metricsService.incrementCounter(
          "audit.log.total", "entity_type", entityType, "action", "CREATE");
      log.debug("Audit log created: {} {} for team {}", CREATE, entityType, teamId);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize entity for audit log", e);
    }
  }

  public void logUpdate(
      UUID teamId,
      String entityType,
      UUID entityId,
      UUID userId,
      @Nullable Object oldEntity,
      Object newEntity,
      Map<String, Object> changedFields) {
    try {
      Map<String, Object> oldValues = objectToMap(oldEntity);
      Map<String, Object> newValues = objectToMap(newEntity);

      auditLogRepository.insertAuditLog(
          UUID.randomUUID(),
          teamId,
          entityType,
          entityId,
          UPDATE.name(),
          JSONB.valueOf(objectMapper.writeValueAsString(changedFields)),
          JSONB.valueOf(objectMapper.writeValueAsString(oldValues)),
          JSONB.valueOf(objectMapper.writeValueAsString(newValues)),
          userId,
          LocalDateTime.now(clock));

      metricsService.incrementCounter(
          "audit.log.total", "entity_type", entityType, "action", "UPDATE");
      log.debug("Audit log created: {} {} for team {}", UPDATE, entityType, teamId);
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
          DELETE.name(),
          null,
          JSONB.valueOf(objectMapper.writeValueAsString(oldValues)),
          null,
          userId,
          LocalDateTime.now(clock));

      metricsService.incrementCounter(
          "audit.log.total", "entity_type", entityType, "action", "DELETE");
      log.debug("Audit log created: {} {} for team {}", DELETE, entityType, teamId);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize entity for audit log", e);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> objectToMap(@Nullable Object obj) {
    if (obj == null) {
      return Map.of();
    }
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
      } else if (oldValue != null && !valuesEqual(oldValue, newValue)) {
        changes.put(key, newValue);
      }
    }

    return changes;
  }

  private boolean valuesEqual(Object a, Object b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    if (a instanceof java.math.BigDecimal da && b instanceof java.math.BigDecimal db) {
      return da.compareTo(db) == 0;
    }
    return a.equals(b);
  }

  public List<RecentActivityResponse> getEntityAuditLog(
      UUID teamId, String entityType, UUID entityId) {
    return auditLogRepository
        .findByTeamIdAndEntityTypeAndEntityId(teamId, entityType, entityId)
        .stream()
        .map(record -> mapRecordToRecentActivity(record, entityType, teamId))
        .toList();
  }

  public PageResponse<RecentActivityResponse> getAllAuditLogsPaginated(
      UUID teamId,
      @Nullable String entityType,
      @Nullable String action,
      @Nullable String search,
      PageRequest pageRequest) {
    PaginatedResult<AuditLogEntry> result =
        auditLogRepository.findAllByTeamIdPaginated(
            teamId, entityType, action, search, pageRequest);
    List<RecentActivityResponse> responses =
        result.items().stream()
            .map(record -> mapRecordToRecentActivity(record, record.entityType(), teamId))
            .toList();
    return PageResponse.of(
        responses, pageRequest.page(), pageRequest.size(), result.totalElements());
  }

  private RecentActivityResponse mapRecordToRecentActivity(
      AuditLogEntry record, String entityType, UUID teamId) {
    String action = record.action();
    @Nullable String firstName = record.firstName();
    @Nullable String lastName = record.lastName();
    String userName =
        (firstName != null && lastName != null) ? firstName + " " + lastName : "Unknown";

    // Parse JSON fields first
    Map<String, Object> changedFields = parseJsonField(record.changedFieldsJson());
    Map<String, Object> oldValues = parseJsonField(record.oldValuesJson());
    Map<String, Object> newValues = parseJsonField(record.newValuesJson());

    // Build description based on action and changed fields
    String description = buildActivityDescription(action, entityType, userName, changedFields);

    // Resolve entity identifier from entity UUID
    UUID entityId = record.entityId();
    String entityIdentifier =
        auditLogRepository
            .findEntityIdentifier(entityType, entityId, teamId)
            .orElse(entityId.toString());

    return new RecentActivityResponse(
        entityType,
        entityIdentifier,
        entityType, // entityName - can be enhanced later
        action,
        Optional.ofNullable(userName),
        record.timestamp().toInstant(UTC),
        Optional.ofNullable(description),
        changedFields,
        oldValues,
        newValues);
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

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseJsonField(@Nullable String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, Map.class);
    } catch (JsonProcessingException e) {
      log.error("Failed to parse JSON field", e);
      return Map.of();
    }
  }

  private String buildActivityDescription(
      String action, String entityType, String userName, Map<String, Object> changedFields) {
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

    // Check for contract party operations
    if (changedFields != null && changedFields.containsKey("partyAdded")) {
      String tenantName = (String) changedFields.get("partyAdded");
      String role = (String) changedFields.get("role");
      return String.format("%s added %s as %s", userName, tenantName, formatRole(role));
    }

    if (changedFields != null && changedFields.containsKey("partyRemoved")) {
      String tenantName = (String) changedFields.get("partyRemoved");
      String role = (String) changedFields.get("role");
      return String.format("%s removed %s (%s)", userName, tenantName, formatRole(role));
    }

    if (changedFields != null && changedFields.containsKey("primaryTenantChanged")) {
      String newTenant = (String) changedFields.get("primaryTenantChanged");
      String oldTenant = (String) changedFields.get("previousPrimaryTenant");
      return String.format(
          "%s changed primary tenant from %s to %s", userName, oldTenant, newTenant);
    }

    // Check for receival operations
    if (changedFields != null && changedFields.containsKey("receivalRegistered")) {
      return String.format(
          "%s registered a receival: %s", userName, changedFields.get("receivalRegistered"));
    }

    if (changedFields != null && changedFields.containsKey("receivalUpdated")) {
      return String.format(
          "%s updated a receival: %s", userName, changedFields.get("receivalUpdated"));
    }

    if (changedFields != null && changedFields.containsKey("receivalDeleted")) {
      return String.format(
          "%s deleted a receival: %s", userName, changedFields.get("receivalDeleted"));
    }

    // Default behavior for other operations
    String actionText =
        switch (action) {
          case "CREATE" -> "created";
          case "UPDATE" -> "updated";
          case "DELETE" -> "deleted";
          case "RESTORE" -> "restored";
          default -> "modified";
        };

    return String.format(
        "%s %s this %s", userName, actionText, entityType.toLowerCase(Locale.ROOT));
  }

  private String formatRole(@Nullable String role) {
    if (role == null) {
      return "Unknown";
    }
    return Arrays.stream(ContractPartyRole.values())
        .filter(r -> r.name().equals(role))
        .findFirst()
        .map(ContractPartyRole::getDisplayName)
        .orElse("Unknown");
  }
}
