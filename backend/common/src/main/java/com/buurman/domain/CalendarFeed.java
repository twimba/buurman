package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarFeed {

  public enum FeedType {
    ALL_PAYMENTS,
    CONTRACT,
    PROPERTY_PAYMENTS,
    TENANT_PAYMENTS
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID userId;
  private String feedToken;
  private FeedType feedType;
  @Builder.Default private Optional<UUID> contractId = Optional.empty();
  @Builder.Default private Optional<UUID> propertyId = Optional.empty();
  @Builder.Default private Optional<UUID> tenantId = Optional.empty();
  @Builder.Default private Boolean enabled = true;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
