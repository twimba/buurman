package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

public class PropertyTenantHistory {

    private UUID id;
    private UUID teamId;
    private UUID propertyId;
    private UUID tenantId;
    private Instant movedInAt;
    private Instant movedOutAt;
    private ActionType actionType;
    private UUID performedBy;
    private Instant performedAt;

    public PropertyTenantHistory() {
    }

    public PropertyTenantHistory(UUID id, UUID teamId, UUID propertyId, UUID tenantId,
                                 Instant movedInAt, Instant movedOutAt, ActionType actionType,
                                 UUID performedBy, Instant performedAt) {
        this.id = id;
        this.teamId = teamId;
        this.propertyId = propertyId;
        this.tenantId = tenantId;
        this.movedInAt = movedInAt;
        this.movedOutAt = movedOutAt;
        this.actionType = actionType;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
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

    public UUID getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(UUID propertyId) {
        this.propertyId = propertyId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getMovedInAt() {
        return movedInAt;
    }

    public void setMovedInAt(Instant movedInAt) {
        this.movedInAt = movedInAt;
    }

    public Instant getMovedOutAt() {
        return movedOutAt;
    }

    public void setMovedOutAt(Instant movedOutAt) {
        this.movedOutAt = movedOutAt;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public UUID getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(UUID performedBy) {
        this.performedBy = performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
    }

    public enum ActionType {
        LINKED,
        UNLINKED
    }
}
