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
public class PropertyTenantHistory {

  private UUID id;
  private UUID teamId;
  private UUID propertyId;
  private UUID tenantId;
  @Builder.Default private Optional<Instant> movedInAt = Optional.empty();
  @Builder.Default private Optional<Instant> movedOutAt = Optional.empty();
  private ActionType actionType;
  private UUID performedBy;
  private Instant performedAt;

  public enum ActionType {
    LINKED,
    UNLINKED
  }
}
