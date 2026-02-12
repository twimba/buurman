package com.buurman.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CalendarFeed {

    public enum FeedType {
        ALL_PAYMENTS,
        CONTRACT,
        PROPERTY_PAYMENTS,
        TENANT_PAYMENTS
    }

    private UUID id;
    private String identifier;
    private UUID teamId;
    private UUID userId;
    private String feedToken;
    private FeedType feedType;
    private UUID contractId;
    private UUID propertyId;
    private UUID tenantId;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant deletedAt;
}
