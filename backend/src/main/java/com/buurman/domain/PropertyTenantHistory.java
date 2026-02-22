package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyTenantHistory {

  private UUID id;
  private UUID teamId;
  private UUID propertyId;
  private UUID tenantId;
  private Instant movedInAt;
  private @Nullable Instant movedOutAt;
  private ActionType actionType;
  private UUID performedBy;
  private Instant performedAt;

  public enum ActionType {
    LINKED,
    UNLINKED
  }
}
