package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
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
  private @Nullable UUID contractId;
  private @Nullable UUID propertyId;
  private @Nullable UUID tenantId;
  private @Nullable Boolean enabled;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  private @Nullable Instant deletedAt;
}
