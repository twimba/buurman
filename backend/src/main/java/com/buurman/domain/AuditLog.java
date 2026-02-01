package com.buurman.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AuditLog {

    private UUID id;
    private UUID teamId;
    private String entityType;
    private UUID entityId;
    private Action action;
    private Map<String, Object> changedFields;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private UUID userId;
    private Instant timestamp;

    public AuditLog() {
    }

    public AuditLog(UUID id, UUID teamId, String entityType, UUID entityId, Action action,
                    Map<String, Object> changedFields, Map<String, Object> oldValues,
                    Map<String, Object> newValues, UUID userId, Instant timestamp) {
        this.id = id;
        this.teamId = teamId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.changedFields = changedFields;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Map<String, Object> getChangedFields() {
        return changedFields;
    }

    public void setChangedFields(Map<String, Object> changedFields) {
        this.changedFields = changedFields;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(Map<String, Object> oldValues) {
        this.oldValues = oldValues;
    }

    public Map<String, Object> getNewValues() {
        return newValues;
    }

    public void setNewValues(Map<String, Object> newValues) {
        this.newValues = newValues;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public enum Action {
        CREATE,
        UPDATE,
        DELETE,
        RESTORE
    }
}
