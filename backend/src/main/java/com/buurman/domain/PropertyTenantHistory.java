package com.buurman.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    public enum ActionType {
        LINKED,
        UNLINKED
    }
}
